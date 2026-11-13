package com.yongyongthon.be.itinerary.data.dto;

import java.util.List;

/**
 * 재추천(#9) 요청. 생성(#1) 요청과 같은 모양에서 바꿀 항목만 보낸다. 생성 요청은 ItineraryCreateRequestDto.
 * 보낸 항목만 검사해야 해서 @Valid를 쓰지 않고 ItineraryConditionValidator가 검사한다 (E4001).
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
