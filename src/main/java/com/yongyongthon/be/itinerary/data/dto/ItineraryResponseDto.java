package com.yongyongthon.be.itinerary.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.enums.SlotType;

import java.util.List;

/**
 * 일정 응답 (지시서 4-1). 필드 이름과 순서를 명세와 같게 둔다.
 * returnRoute 필드는 없다. 귀가 정보는 slots의 마지막 항목(type=return).
 */
public record ItineraryResponseDto(
        String itineraryId,
        String title,
        String baseEventId,
        String date,
        String transport,
        TimeWindow timeWindow,
        int totalCost,
        int costUnknownCount,
        String coverImageUrl,
        @JsonProperty("isSaved") boolean isSaved,
        List<SlotResponseDto> slots,
        List<WarningResponseDto> warnings,
        Integer crowdLevel,
        String crowdLabel
) {

    public record TimeWindow(String start, String end) {
    }

    public static ItineraryResponseDto of(Itinerary it, List<WarningResponseDto> warnings, boolean saved) {
        List<ItinerarySlot> ordered = it.orderedSlots();
        return new ItineraryResponseDto(
                it.getItineraryId(),
                it.getTitle(),
                it.getBaseEventId(),
                it.getDate().toString(),
                it.getTransport(),
                new TimeWindow(it.getWindowStart(), it.getWindowEnd()),
                totalCost(ordered),
                costUnknownCount(ordered),
                it.getCoverImageUrl(),
                saved,
                ordered.stream().map(SlotResponseDto::from).toList(),
                warnings,
                null,
                null
        );
    }

    /** costAmount가 null이 아닌 슬롯의 합계 */
    public static int totalCost(List<ItinerarySlot> slots) {
        return slots.stream()
                .filter(s -> s.getCostAmount() != null)
                .mapToInt(ItinerarySlot::getCostAmount)
                .sum();
    }

    /** costAmount가 null인 슬롯 수. 귀가 슬롯은 세지 않음 */
    public static int costUnknownCount(List<ItinerarySlot> slots) {
        return (int) slots.stream()
                .filter(s -> !SlotType.RETURN.equals(s.getType()))
                .filter(s -> s.getCostAmount() == null)
                .count();
    }
}
