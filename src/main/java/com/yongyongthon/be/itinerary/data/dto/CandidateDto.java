package com.yongyongthon.be.itinerary.data.dto;

/**
 * 교체 후보 한 건 (지시서 7장 #6, #7). 행사 후보와 식사 후보가 같은 모양을 쓴다.
 */
public record CandidateDto(
        String candidateId,
        String kind,
        String eventId,
        String name,
        String category,
        String placeName,
        Double lat,
        Double lng,
        String time,
        Integer durationMin,
        Integer costAmount,
        String costText,
        Double distanceKmFromPrev,
        Integer estimatedMinutes,
        String description,
        String source
) {
}
