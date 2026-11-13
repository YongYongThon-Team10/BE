package com.yongyongthon.be.event.service;

import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.EventProgram;
import com.yongyongthon.be.event.data.dto.NearbyEvent;
import com.yongyongthon.be.event.data.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 일정(S5)과 내 코스(S6)가 행사를 읽는 곳. events 테이블(EventRepository)만 읽는다.
 * 행사 수가 수천 건 수준이라 근처 행사는 전체를 읽어 거리와 기간을 메모리에서 계산한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventCatalog {

    private final EventRepository eventRepository;

    public Optional<EventInfo> findById(String eventId) {
        if (eventId == null) {
            return Optional.empty();
        }
        return eventRepository.findById(eventId).map(EventInfo::from);
    }

    /**
     * 근처 행사 후보 (지시서 6-5). 거리 가까운 순.
     * 1. date가 행사 기간에 들어감 (기간이 null이면 제외)
     * 2. 좌표가 있음
     * 3. excludeIds에 없음
     * 4. (lat, lng)에서 radiusKm 이하
     * 5. freeOnly이면 무료로 확인된 행사만
     */
    public List<NearbyEvent> findNearby(LocalDate date, double lat, double lng, double radiusKm,
                                        boolean freeOnly, Set<String> excludeIds) {
        return eventRepository.findAll().stream()
                .map(EventInfo::from)
                .filter(e -> e.runsOn(date))
                .filter(EventInfo::hasCoords)
                .filter(e -> !excludeIds.contains(e.eventId()))
                .filter(e -> !freeOnly || e.free())
                .map(e -> new NearbyEvent(e, Geo.haversineKm(lat, lng, e.lat(), e.lng())))
                .filter(n -> n.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyEvent::distanceKm)
                        .thenComparing(n -> n.event().eventId()))
                .toList();
    }

    public List<EventProgram> findPrograms(String eventId) {
        return findById(eventId).map(EventInfo::programsOrEmpty).orElse(List.of());
    }

    /** 기간이 [from, to]와 겹치는 행사 중 ids에 든 것 (캘린더용) */
    public List<EventInfo> findByIdsOverlapping(Set<String> ids, LocalDate from, LocalDate to) {
        return eventRepository.findAllById(ids).stream()
                .map(EventInfo::from)
                .filter(e -> e.overlaps(from, to))
                .sorted(Comparator.comparing(EventInfo::startDate).thenComparing(EventInfo::eventId))
                .toList();
    }
}
