package com.yongyongthon.be.event.service;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.response.RequestValueInvalidException;
import com.yongyongthon.be.event.data.dto.EventDetailResponseDto;
import com.yongyongthon.be.event.data.dto.EventDiscrepancyResponseDto;
import com.yongyongthon.be.event.data.dto.EventListResponseDto;
import com.yongyongthon.be.event.data.dto.EventSearchRequestDto;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.data.entity.EventSource;
import com.yongyongthon.be.event.data.exception.DistanceCoordinateRequiredException;
import com.yongyongthon.be.event.data.exception.EventNotFoundException;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.event.data.repository.EventSourceRepository;
import com.yongyongthon.be.event.data.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private static final String APPLY_TYPE_APPLY = "apply";
    private static final String DATE_STATUS_ANNUAL = "연간 계획";
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final EventRepository eventRepository;
    private final EventSourceRepository eventSourceRepository;
    private final FavoriteRepository favoriteRepository;

    @Transactional(readOnly = true)
    public Event getEventEntity(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("[getEventEntity] 행사 조회 실패: {}", eventId);
                    return new EventNotFoundException();
                });
    }

    @Transactional(readOnly = true)
    public GlobalResponse<EventDetailResponseDto> getEvent(String eventId, String email) {
        Event event = getEventEntity(eventId);

        List<EventDetailResponseDto.Source> sources = eventSourceRepository.findByEventIdOrderBySortOrder(eventId)
                .stream()
                // 공식 출처를 먼저, 같은 유형 안에서는 엑셀 순서
                .sorted(Comparator.comparing((EventSource source) -> !"official".equals(source.getType())))
                .map(source -> EventDetailResponseDto.Source.builder()
                        .name(source.getName())
                        .type(source.getType())
                        .url(source.getUrl())
                        .build())
                .toList();

        return GlobalResponse.ok(EventDetailResponseDto.builder()
                .eventId(event.getId())
                .name(event.getName())
                .category(event.getCategory())
                .subCategory(event.getSubCategory())
                .district(event.getDistrict())
                .tags(tagsOf(event))
                // 출처별 값 데이터가 생기기 전까지는 항상 false (E3와 같음)
                .hasDiscrepancy(false)
                // 비로그인이면 false
                .isLiked(email != null && favoriteRepository.existsByUserEmailAndEventId(email, eventId))
                .keyInfo(EventDetailResponseDto.KeyInfo.builder()
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .dateStatus(event.getDateStatus())
                        .time(event.getTime())
                        .placeName(event.getPlaceName())
                        .address(event.getAddress())
                        .district(event.getDistrict())
                        .lat(event.getLat())
                        .lng(event.getLng())
                        .coordPrecision(event.getCoordPrecision())
                        .fee(event.getFee())
                        .isFree(event.getIsFree())
                        .target(event.getTarget())
                        .build())
                .apply(EventDetailResponseDto.Apply.builder()
                        .type(event.getApplyType())
                        .text(APPLY_TYPE_APPLY.equals(event.getApplyType()) ? "신청 필요" : null)
                        .url(event.getApplyUrl())
                        .receiptStart(event.getReceiptStart())
                        .receiptEnd(event.getReceiptEnd())
                        .status(event.getReceiptStatus())
                        .build())
                .programs(List.of())
                .sources(sources)
                .build());
    }

    // ponytail: 행사 전체(약 1,800건)를 읽어 메모리에서 거르고 정렬한다. 수만 건이 되면 DB 쿼리(Specification)로 옮긴다
    @Transactional(readOnly = true)
    public GlobalResponse<EventListResponseDto> searchEvents(EventSearchRequestDto request, String email) {
        LocalDate from = request.getDateFrom();
        LocalDate to = request.getDateTo() != null ? request.getDateTo() : from;
        if (from.isAfter(to)) {
            log.warn("[searchEvents] 조회 기간 역전: {} ~ {}", from, to);
            throw new RequestValueInvalidException();
        }
        boolean hasOrigin = request.getLat() != null && request.getLng() != null;
        if ("distance".equals(request.getSort()) && !hasOrigin) {
            throw new DistanceCoordinateRequiredException();
        }

        Set<String> likedIds = email == null ? Set.of() : favoriteRepository.findEventIdsByUserEmail(email);
        List<EventListResponseDto.Item> matched = eventRepository.findAll().stream()
                .filter(event -> overlaps(event, from, to))
                .filter(event -> isEmpty(request.getDistrict()) || request.getDistrict().contains(event.getDistrict()))
                .filter(event -> isEmpty(request.getDong()) || containsAnyDong(event.getAddress(), request.getDong()))
                .filter(event -> !"free".equals(request.getBudget()) || Boolean.TRUE.equals(event.getIsFree()))
                .sorted(byDate(from))
                .map(event -> toItem(event, hasOrigin
                        ? distanceKm(request.getLat(), request.getLng(), event.getLat(), event.getLng())
                        : null, likedIds.contains(event.getId())))
                .toList();

        if ("distance".equals(request.getSort())) {
            // 정렬이 안정적이므로 거리가 같으면 날짜순이 유지된다. 좌표가 없는 행사는 맨 뒤
            matched = matched.stream()
                    .sorted(Comparator.comparing(EventListResponseDto.Item::getDistanceKm,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }

        if ("map".equals(request.getView())) {
            return GlobalResponse.ok(EventListResponseDto.builder()
                    .totalCount(matched.size()).page(0).size(matched.size()).events(matched)
                    .build());
        }

        int fromIndex = (int) Math.min((long) request.getPage() * request.getSize(), matched.size());
        int toIndex = Math.min(fromIndex + request.getSize(), matched.size());
        return GlobalResponse.ok(EventListResponseDto.builder()
                .totalCount(matched.size()).page(request.getPage()).size(request.getSize())
                .events(matched.subList(fromIndex, toIndex))
                .build());
    }

    // 행사 기간이 조회 기간과 하루라도 겹치면 포함. 날짜가 없는 행사는 겹침을 판단할 수 없어 제외
    private boolean overlaps(Event event, LocalDate from, LocalDate to) {
        if (event.getStartDate() == null || event.getEndDate() == null) {
            return false;
        }
        return !event.getStartDate().isAfter(to) && !event.getEndDate().isBefore(from);
    }

    // 명세 S1-7: 주소에 동 이름이 들어 있는 행사만. 주소가 없으면 제외
    private boolean containsAnyDong(String address, List<String> dongs) {
        return address != null && dongs.stream().anyMatch(address::contains);
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    // 진행 중(조회 시작일에 이미 시작), 시작 예정, 연간 계획, 날짜 없음 순. 같은 묶음 안에서는 시작일, 행사ID 순
    private Comparator<Event> byDate(LocalDate from) {
        return Comparator.comparingInt((Event event) -> {
                    if (event.getStartDate() == null) {
                        return 3;
                    }
                    if (DATE_STATUS_ANNUAL.equals(event.getDateStatus())) {
                        return 2;
                    }
                    return event.getStartDate().isAfter(from) ? 1 : 0;
                })
                .thenComparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Event::getId);
    }

    // 직선거리(하버사인), 소수 둘째 자리. 어느 한쪽 좌표라도 없으면 null
    static Double distanceKm(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        double km = 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
        return Math.round(km * 100) / 100.0;
    }

    private EventListResponseDto.Item toItem(Event event, Double distanceKm, boolean isLiked) {
        return EventListResponseDto.Item.builder()
                .eventId(event.getId())
                .name(event.getName())
                .category(event.getCategory())
                .subCategory(event.getSubCategory())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .dateStatus(event.getDateStatus())
                .time(event.getTime())
                .placeName(event.getPlaceName())
                .address(event.getAddress())
                .district(event.getDistrict())
                .lat(event.getLat())
                .lng(event.getLng())
                .coordPrecision(event.getCoordPrecision())
                .fee(event.getFee())
                .isFree(event.getIsFree())
                .applyType(event.getApplyType())
                .applyUrl(event.getApplyUrl())
                .tags(tagsOf(event))
                .sourceUrl(event.getSourceUrl())
                .distanceKm(distanceKm)
                // 상세(E2)와 같은 이유로 고정값
                .hasDiscrepancy(false)
                .isLiked(isLiked)
                .build();
    }

    // 서버는 어느 값이 맞는지 판단하지 않고 차이가 있다는 사실만 알린다 (명세 S2-5)
    @Transactional(readOnly = true)
    public GlobalResponse<EventDiscrepancyResponseDto> getDiscrepancies(String eventId) {
        Event event = getEventEntity(eventId);
        return GlobalResponse.ok(EventDiscrepancyResponseDto.builder()
                .eventId(event.getId())
                // 출처별 값 데이터가 생기기 전까지 고정값. E1, E2의 hasDiscrepancy와 함께 바꾼다
                .hasDiscrepancy(false)
                .items(List.of())
                .build());
    }

    // 카드와 상세에 붙는 태그. 확인된 값만 쓰고 "안내 상이"는 넣지 않는다 (배지는 hasDiscrepancy로 표시)
    List<String> tagsOf(Event event) {
        List<String> tags = new ArrayList<>();
        if (Boolean.TRUE.equals(event.getIsFree())) {
            tags.add("무료");
        }
        if (APPLY_TYPE_APPLY.equals(event.getApplyType())) {
            tags.add("신청 필요");
        }
        return tags;
    }
}
