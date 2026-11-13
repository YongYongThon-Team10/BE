package com.yongyongthon.be.itinerary.data.dto;

/**
 * 슬롯 추가 요청 (지시서 7장 #3).
 */
public record SlotAddRequestDto(
        String type,
        String eventId,
        String programId,
        String title,
        Integer durationMin,
        String costText,
        String address,
        String afterSlotId
) {
}
