package com.yongyongthon.be.itinerary.data.dto;

/**
 * 슬롯 교체 요청 (지시서 7장 #5). 둘 중 하나만 보낸다.
 */
public record SlotReplaceRequestDto(
        String replaceWithCandidateId,
        String replaceWithEventId
) {
}
