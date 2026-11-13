package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.common.util.Times;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.itinerary.data.dto.WarningResponseDto;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.enums.SlotType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 재계산 (지시서 6-2). 모든 수정 API가 마지막에 호출한다.
 * 순서, 시간, 이동, 귀가 슬롯, timeBasis를 슬롯에 다시 쓰고 경고 목록을 돌려준다.
 * 비용 합계는 응답을 만들 때 슬롯에서 계산한다.
 * 지시서와 다른 점: 기준 행사에 공식 시간이 있으면 그 시작 시각보다 일찍 배치하지 않는다 (PROGRESS.md 참고).
 */
@Component
@RequiredArgsConstructor
public class Recalculator {

    public static final String RETURN_TITLE = "집으로 돌아가기";
    private static final String ORIGIN_LINK_NAME = "출발지";
    private static final String KAKAO_LINK = "https://map.kakao.com/link/to/";

    private final EventRepository eventRepository;
    private final AppProperties props;

    public List<WarningResponseDto> recalculate(Itinerary it) {
        String mode = it.getTransport();
        double speed = props.speedOf(mode);
        int cursor = Times.toMinutes(it.getWindowStart());
        double prevLat = it.getOriginLat();
        double prevLng = it.getOriginLng();
        int[] baseRange = eventRepository.findById(it.getBaseEventId())
                .map(e -> Times.parseRange(e.time()))
                .orElse(null);

        List<WarningResponseDto> noNearby = new ArrayList<>();
        List<WarningResponseDto> timeUnknown = new ArrayList<>();
        List<WarningResponseDto> coordMissing = new ArrayList<>();

        List<ItinerarySlot> ordered = it.orderedSlots();
        ItinerarySlot last = null;
        ItinerarySlot returnSlot = null;
        int order = 1;
        for (ItinerarySlot s : ordered) {
            s.setSlotOrder(order++);
            last = s;
            if (SlotType.RETURN.equals(s.getType())) {
                returnSlot = s;
                continue;
            }

            // 비어 있는 근처 행사 슬롯: 시간과 이동 없이 자리만 둔다
            if (isEmptyNearby(s)) {
                s.clearMove();
                s.setStartTime(null);
                s.setEndTime(null);
                s.setTimeBasis(ItinerarySlot.SUGGESTED);
                noNearby.add(WarningResponseDto.noNearbyEvent(s.getSlotId()));
                continue;
            }

            if (s.hasCoords()) {
                double km = Geo.haversineKm(prevLat, prevLng, s.getPlaceLat(), s.getPlaceLng());
                int minutes = Geo.estimateMinutes(km, speed);
                s.setMove(mode, Geo.round1(km), minutes, mapLink(s.getPlaceName(), s.getPlaceLat(), s.getPlaceLng()));
                cursor += minutes;
                prevLat = s.getPlaceLat();
                prevLng = s.getPlaceLng();
            } else {
                s.clearMove();
                // 장소 자체가 없는 슬롯(빈 식사 등)은 좌표 누락으로 보지 않는다
                if (s.hasPlace()) {
                    coordMissing.add(WarningResponseDto.coordMissing(s.getSlotId()));
                }
            }

            // 기준 행사에 공식 시간("HH:mm ~ HH:mm")이 있고 아직 시작 전이면 시작 시각까지 기다린다
            if (SlotType.EVENT.equals(s.getType()) && baseRange != null && cursor < baseRange[0]) {
                cursor = baseRange[0];
            }

            int duration = s.getDurationMin() == null ? 0 : s.getDurationMin();
            int start = cursor;
            int end = cursor + duration;
            s.setStartTime(Times.format(start));
            s.setEndTime(Times.format(end));
            cursor = end;

            boolean official = SlotType.EVENT.equals(s.getType()) && baseRange != null
                    && start >= baseRange[0] && end <= baseRange[1];
            s.setTimeBasis(official ? ItinerarySlot.OFFICIAL : ItinerarySlot.SUGGESTED);

            if (SlotType.FROM_EVENT_DB.contains(s.getType()) && s.getEventId() != null) {
                Optional<EventInfo> source = eventRepository.findById(s.getEventId());
                if (source.isPresent() && source.get().time() == null) {
                    timeUnknown.add(WarningResponseDto.timeUnknown(s.getSlotId()));
                }
            }
        }

        int lastEnd = cursor;
        if (returnSlot != null) {
            applyReturn(it, returnSlot, prevLat, prevLng, cursor, mode, speed);
            lastEnd = Times.toMinutes(returnSlot.getEndTime());
        }

        List<WarningResponseDto> warnings = new ArrayList<>();
        if (last != null && lastEnd > Times.toMinutes(it.getWindowEnd())) {
            warnings.add(WarningResponseDto.timeExceeded(it.getWindowStart(), it.getWindowEnd(), last.getSlotId()));
        }
        warnings.addAll(noNearby);
        warnings.addAll(timeUnknown);
        warnings.addAll(coordMissing);
        return warnings;
    }

    /** 귀가 슬롯: place는 출발지, 직전 위치에서 출발지까지 이동 */
    private void applyReturn(Itinerary it, ItinerarySlot s, double prevLat, double prevLng,
                             int cursor, String mode, double speed) {
        s.setTitle(RETURN_TITLE);
        s.setPlace(it.getOriginAddress(), it.getOriginAddress(), it.getOriginLat(), it.getOriginLng());
        s.setCostAmount(null);
        s.setCostText(null);
        s.setTimeBasis(ItinerarySlot.SUGGESTED);
        double km = Geo.haversineKm(prevLat, prevLng, it.getOriginLat(), it.getOriginLng());
        int minutes = Geo.estimateMinutes(km, speed);
        s.setMove(mode, Geo.round1(km), minutes, mapLink(it.getOriginAddress(), it.getOriginLat(), it.getOriginLng()));
        s.setDurationMin(minutes);
        s.setStartTime(Times.format(cursor));
        s.setEndTime(Times.format(cursor + minutes));
    }

    public static boolean isEmptyNearby(ItinerarySlot s) {
        return SlotType.NEARBY_EVENT.equals(s.getType()) && s.getEventId() == null;
    }

    /** https://map.kakao.com/link/to/{이름},{lat},{lng} (지시서 11장 #8, 임시) */
    static String mapLink(String name, double lat, double lng) {
        String label = name == null || name.isBlank() ? ORIGIN_LINK_NAME : name;
        String encoded = URLEncoder.encode(label, StandardCharsets.UTF_8).replace("+", "%20");
        return KAKAO_LINK + encoded + "," + lat + "," + lng;
    }
}
