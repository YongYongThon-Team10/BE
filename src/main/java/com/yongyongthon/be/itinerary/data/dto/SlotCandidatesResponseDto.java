package com.yongyongthon.be.itinerary.data.dto;

import java.util.List;

/**
 * 후보 조회 응답 (지시서 7장 #6, #7).
 */
public record SlotCandidatesResponseDto(
        String slotId,
        String type,
        List<CandidateDto> candidates
) {
}
