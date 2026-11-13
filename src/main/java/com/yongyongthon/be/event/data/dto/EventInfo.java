package com.yongyongthon.be.event.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 생성에 쓰는 행사 필드 (지시서 8장).
 * duplicateOf는 원본에서 '중복 확인'으로 표시된 행이 가리키는 대표 행사 ID다. 근처 후보에서 빼는 데만 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventInfo(
        String eventId,
        String name,
        String category,
        LocalDate startDate,
        LocalDate endDate,
        String time,
        String placeName,
        String address,
        Double lat,
        Double lng,
        String fee,
        @JsonProperty("isFree") Boolean isFree,
        String summary,
        String thumbnailUrl,
        Integer runningTimeMin,
        List<EventProgram> programs,
        String duplicateOf
) {

    public boolean hasCoords() {
        return lat != null && lng != null;
    }

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
