package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.EventProgram;
import com.yongyongthon.be.event.data.dto.NearbyEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 행사 DB 읽기 전용 인터페이스 (지시서 8장).
 * 지금은 엑셀을 바꾼 JSON 파일 구현. 행사 DB(PostgreSQL) 구조가 정해지면 DB 구현으로 갈아끼운다.
 */
public interface EventRepository {

    Optional<EventInfo> findById(String eventId);

    /**
     * 근처 행사 후보 (지시서 6-5). 거리 가까운 순.
     * 1. date가 행사 기간에 들어감 (기간이 null이면 제외)
     * 2. 좌표가 있음
     * 3. excludeIds에 없고, 다른 행사의 중복 행이 아님
     * 4. (lat, lng)에서 radiusKm 이하
     * 5. freeOnly이면 isFree = true만
     */
    List<NearbyEvent> findNearby(LocalDate date, double lat, double lng, double radiusKm,
                                 boolean freeOnly, Set<String> excludeIds);

    List<EventProgram> findPrograms(String eventId);

    /** 기간이 [from, to]와 겹치는 행사 중 ids에 든 것 (캘린더용) */
    List<EventInfo> findByIdsOverlapping(Set<String> ids, LocalDate from, LocalDate to);
}
