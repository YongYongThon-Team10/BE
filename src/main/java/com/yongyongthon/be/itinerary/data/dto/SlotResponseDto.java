package com.yongyongthon.be.itinerary.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;

/**
 * 일정 항목 응답 (지시서 4-2). 필드 이름과 순서를 명세와 같게 둔다.
 */
public record SlotResponseDto(
        String slotId,
        int order,
        String type,
        String title,
        String eventId,
        String startTime,
        String endTime,
        Integer durationMin,
        Integer costAmount,
        String costText,
        String description,
        String timeBasis,
        Place place,
        @JsonProperty("isUserAdded") boolean isUserAdded,
        Move moveFromPrev
) {

    public record Place(String name, String address, Double lat, Double lng) {
    }

    public record Move(String mode, Double distanceKm, Integer estimatedMinutes,
                       @JsonProperty("isEstimate") boolean isEstimate, String mapLink) {
    }

    public static SlotResponseDto from(ItinerarySlot s) {
        Place place = s.hasPlace()
                ? new Place(s.getPlaceName(), s.getPlaceAddress(), s.getPlaceLat(), s.getPlaceLng())
                : null;
        Move move = s.hasMove()
                ? new Move(s.getMoveMode(), s.getMoveDistanceKm(), s.getMoveEstimatedMin(), true, s.getMoveMapLink())
                : null;
        return new SlotResponseDto(
                s.getSlotId(),
                s.getSlotOrder(),
                s.getType(),
                s.getTitle(),
                s.getEventId(),
                s.getStartTime(),
                s.getEndTime(),
                s.getDurationMin(),
                s.getCostAmount(),
                s.getCostText(),
                s.getDescription(),
                s.getTimeBasis(),
                place,
                s.isUserAdded(),
                move
        );
    }
}
