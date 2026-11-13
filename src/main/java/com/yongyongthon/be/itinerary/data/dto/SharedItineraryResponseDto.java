package com.yongyongthon.be.itinerary.data.dto;

import java.util.List;

/**
 * 공유 일정 미리보기 (지시서 7장 #11). 읽기 전용이라 slotId와 출발지 정보를 넣지 않는다.
 */
public record SharedItineraryResponseDto(
        String shareId,
        String title,
        String date,
        String companion,
        int slotCount,
        int totalCost,
        int costUnknownCount,
        String coverImageUrl,
        List<SharedSlot> slots
) {

    public record SharedSlot(
            int order,
            String type,
            String title,
            String startTime,
            String endTime,
            Integer durationMin,
            String costText,
            SharedPlace place
    ) {
    }

    public record SharedPlace(String name, Double lat, Double lng) {
    }
}
