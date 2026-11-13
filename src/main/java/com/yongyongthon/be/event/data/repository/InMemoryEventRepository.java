package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.EventProgram;
import com.yongyongthon.be.event.data.dto.NearbyEvent;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 시작할 때 행사 전체를 읽어 메모리에 두고 조회하는 공통 구현.
 * 행사 수가 수천 건 수준이라 조회마다 DB를 부르지 않고 메모리에서 거리와 기간을 계산한다.
 * 읽는 곳(JSON 파일, PostgreSQL)만 하위 클래스가 정한다.
 */
public abstract class InMemoryEventRepository implements EventRepository {

    private final Map<String, EventInfo> events;

    protected InMemoryEventRepository(List<EventInfo> loaded) {
        Map<String, EventInfo> map = new LinkedHashMap<>();
        for (EventInfo e : loaded) {
            if (e.eventId() != null) {
                map.put(e.eventId(), e);
            }
        }
        this.events = Collections.unmodifiableMap(map);
    }

    protected int size() {
        return events.size();
    }

    @Override
    public Optional<EventInfo> findById(String eventId) {
        return Optional.ofNullable(eventId == null ? null : events.get(eventId));
    }

    @Override
    public List<NearbyEvent> findNearby(LocalDate date, double lat, double lng, double radiusKm,
                                        boolean freeOnly, Set<String> excludeIds) {
        return events.values().stream()
                .filter(e -> e.runsOn(date))
                .filter(EventInfo::hasCoords)
                .filter(e -> !excludeIds.contains(e.eventId()))
                .filter(e -> e.duplicateOf() == null)
                .filter(e -> !freeOnly || e.free())
                .map(e -> new NearbyEvent(e, Geo.haversineKm(lat, lng, e.lat(), e.lng())))
                .filter(n -> n.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyEvent::distanceKm)
                        .thenComparing(n -> n.event().eventId()))
                .toList();
    }

    @Override
    public List<EventProgram> findPrograms(String eventId) {
        return findById(eventId).map(EventInfo::programsOrEmpty).orElse(List.of());
    }

    @Override
    public List<EventInfo> findByIdsOverlapping(Set<String> ids, LocalDate from, LocalDate to) {
        return ids.stream()
                .map(events::get)
                .filter(e -> e != null && e.overlaps(from, to))
                .sorted(Comparator.comparing(EventInfo::startDate).thenComparing(EventInfo::eventId))
                .toList();
    }
}
