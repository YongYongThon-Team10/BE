package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.NearbyEvent;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 근처 행사 후보 (지시서 6-5). 생성과 alternatives가 같이 쓴다.
 * 지시서 규칙에 더해, 실제 데이터에서 맞지 않는 후보를 뺀다 (PROGRESS.md 참고).
 * 1. 제외 분류(기본: 도서관 프로그램)와 제외 단어(기본: 모집)
 * 2. 일정에 이미 있는 행사와 같은 날, 같은 장소, 비슷한 이름인 행사 (원본에서 중복 표시가 빠진 것)
 */
@Component
@RequiredArgsConstructor
public class NearbyFinder {

    /** 같은 장소로 볼 거리 (km) */
    private static final double SAME_PLACE_KM = 0.1;
    /** 이름 앞부분이 이만큼(글자 수) 같으면 같은 행사로 본다 */
    private static final int SAME_NAME_PREFIX = 8;

    private final EventRepository eventRepository;
    private final AppProperties props;

    /**
     * (lat, lng)에서 가까운 순 후보.
     * 기준 행사, 일정에 이미 들어 있는 행사, 기준 행사의 중복 대표 행사는 뺀다.
     */
    public List<NearbyEvent> find(Itinerary it, EventInfo base, double lat, double lng) {
        Set<String> exclude = new HashSet<>();
        List<EventInfo> inItinerary = new ArrayList<>();
        exclude.add(base.eventId());
        inItinerary.add(base);
        if (base.duplicateOf() != null) {
            exclude.add(base.duplicateOf());
        }
        for (ItinerarySlot s : it.getSlots()) {
            if (s.getEventId() != null && exclude.add(s.getEventId())) {
                eventRepository.findById(s.getEventId()).ifPresent(inItinerary::add);
            }
        }
        boolean freeOnly = "free".equals(it.getBudget());
        List<NearbyEvent> found = eventRepository.findNearby(
                it.getDate(), lat, lng, props.getNearbyRadiusKm(), freeOnly, exclude);

        List<String> excludedCategories = props.nearbyExcludedCategoryList();
        List<String> excludedKeywords = props.nearbyExcludedKeywordList();
        List<NearbyEvent> result = new ArrayList<>();
        List<EventInfo> kept = new ArrayList<>(inItinerary);
        for (NearbyEvent n : found) {
            EventInfo e = n.event();
            if (e.category() != null && excludedCategories.contains(e.category())) {
                continue;
            }
            if (e.name() != null && excludedKeywords.stream().anyMatch(e.name()::contains)) {
                continue;
            }
            if (kept.stream().anyMatch(k -> looksSame(k, e))) {
                continue;
            }
            kept.add(e);
            result.add(n);
        }
        return result;
    }

    /** 같은 기간, 같은 장소(0.1km 이내), 이름 앞 8글자 이상이 같으면 같은 행사로 본다 */
    static boolean looksSame(EventInfo a, EventInfo b) {
        if (!Objects.equals(a.startDate(), b.startDate()) || !Objects.equals(a.endDate(), b.endDate())) {
            return false;
        }
        if (!a.hasCoords() || !b.hasCoords()
                || Geo.haversineKm(a.lat(), a.lng(), b.lat(), b.lng()) > SAME_PLACE_KM) {
            return false;
        }
        String na = normalize(a.name());
        String nb = normalize(b.name());
        int common = 0;
        while (common < na.length() && common < nb.length() && na.charAt(common) == nb.charAt(common)) {
            common++;
        }
        return common >= SAME_NAME_PREFIX;
    }

    private static String normalize(String name) {
        return name == null ? "" : name.replaceAll("[^0-9A-Za-z가-힣]", "").toLowerCase();
    }

    /**
     * 이전 슬롯 기준점: target 앞에서 좌표가 있는 가장 가까운 슬롯, 없으면 기준 행사.
     * 기준 행사에도 좌표가 없으면 null.
     */
    public double[] anchorBefore(Itinerary it, ItinerarySlot target, EventInfo base) {
        ItinerarySlot found = null;
        for (ItinerarySlot s : it.orderedSlots()) {
            if (s == target) {
                break;
            }
            if (s.hasCoords()) {
                found = s;
            }
        }
        if (found != null) {
            return new double[]{found.getPlaceLat(), found.getPlaceLng()};
        }
        return base.hasCoords() ? new double[]{base.lat(), base.lng()} : null;
    }
}
