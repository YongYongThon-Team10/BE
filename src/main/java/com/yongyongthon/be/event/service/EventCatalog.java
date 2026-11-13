package com.yongyongthon.be.event.service;

import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.EventProgram;
import com.yongyongthon.be.event.data.dto.NearbyEvent;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.data.repository.EventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        return allEventsWithoutFlush().stream()
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

    /**
     * 행사 전체. 일정을 짜는 도중(재추천)에 불리면 아직 slotId가 없는 슬롯이 영속성 컨텍스트에 있어서,
     * 조회 전 자동 flush가 일어나면 slot_id NOT NULL 위반이 난다. 행사 테이블만 읽으므로 flush하지 않는다.
     */
    private List<Event> allEventsWithoutFlush() {
        return entityManager.createQuery("select e from Event e", Event.class)
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
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
