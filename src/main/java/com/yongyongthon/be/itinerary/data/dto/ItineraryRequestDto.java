package com.yongyongthon.be.itinerary.data.dto;

import java.util.List;

/**
 * 일정 생성 요청 (지시서 7장 #1). 재추천(#9)은 같은 모양에서 바꿀 항목만 보낸다.
 * 검증은 ItineraryRequestValidator가 한다 (오류 코드를 C001로 맞추기 위해 @Valid를 쓰지 않음).
 */
public record ItineraryRequestDto(
        String baseEventId,
        String date,
        Origin origin,
        String transport,
        TimeWindow timeWindow,
        String companion,
        Integer kidsAge,
        String budget,
        List<String> wants
) {

    public record Origin(String type, Double lat, Double lng, String address) {
    }

    public record TimeWindow(String start, String end) {
    }
}
