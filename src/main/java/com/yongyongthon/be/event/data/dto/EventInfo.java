package com.yongyongthon.be.event.data.dto;

import com.yongyongthon.be.event.data.entity.Event;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 생성에 쓰는 행사 필드 (지시서 8장). events 테이블(Event 엔티티)에서 만든다.
 * category는 공연 또는 행사, subCategory는 원본 분류(도서관 프로그램, 전시, 축제 등)다.
 * summary, thumbnailUrl, runningTimeMin, programs는 행사 데이터에 아직 없어서 null 또는 빈 목록이다.
 */
public record EventInfo(
        String eventId,
        String name,
        String category,
        String subCategory,
        LocalDate startDate,
        LocalDate endDate,
        String time,
        String placeName,
        String address,
        Double lat,
        Double lng,
        String fee,
        Boolean isFree,
        String summary,
        String thumbnailUrl,
        Integer runningTimeMin,
        List<EventProgram> programs
) {

    public static EventInfo from(Event e) {
        return new EventInfo(
                e.getId(), e.getName(), e.getCategory(), e.getSubCategory(),
                e.getStartDate(), e.getEndDate(), e.getTime(),
                e.getPlaceName(), e.getAddress(), e.getLat(), e.getLng(),
                e.getFee(), e.getIsFree(),
                null, null, null, List.of());
    }

    public boolean hasCoords() {
        return lat != null && lng != null;
    }

    /** 무료로 확인된 행사만 true. 모르면(null) false */
    public boolean free() {
        return Boolean.TRUE.equals(isFree);
    }

    /** 기간이 모두 있고 date가 그 안에 들어가면 true */
    public boolean runsOn(LocalDate date) {
        return startDate != null && endDate != null
                && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** 기간이 조회 범위 [from, to]와 겹치면 true */
    public boolean overlaps(LocalDate from, LocalDate to) {
        return startDate != null && endDate != null
                && !endDate.isBefore(from) && !startDate.isAfter(to);
    }

    public List<EventProgram> programsOrEmpty() {
        return programs == null ? List.of() : programs;
    }
}
