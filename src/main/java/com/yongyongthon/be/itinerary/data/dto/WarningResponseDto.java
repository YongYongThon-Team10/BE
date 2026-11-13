package com.yongyongthon.be.itinerary.data.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 경고 (지시서 4-3). slotId는 해당 슬롯이 있을 때만 포함한다.
 */
public record WarningResponseDto(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String slotId
) {

    public static final String TIME_EXCEEDED = "TIME_EXCEEDED";
    public static final String NO_NEARBY_EVENT = "NO_NEARBY_EVENT";
    public static final String TIME_UNKNOWN = "TIME_UNKNOWN";
    public static final String COORD_MISSING = "COORD_MISSING";

    public static WarningResponseDto timeExceeded(String start, String end, String slotId) {
        return new WarningResponseDto(TIME_EXCEEDED, "설정한 시간대(" + start + "~" + end + ")를 넘었어요.", slotId);
    }

    public static WarningResponseDto noNearbyEvent(String slotId) {
        return new WarningResponseDto(NO_NEARBY_EVENT, "조건에 맞는 근처 행사가 없어요.", slotId);
    }

    public static WarningResponseDto timeUnknown(String slotId) {
        return new WarningResponseDto(TIME_UNKNOWN, "행사 시간 정보가 없어 시간을 제안값으로 배치했어요.", slotId);
    }

    public static WarningResponseDto coordMissing(String slotId) {
        return new WarningResponseDto(COORD_MISSING, "위치 정보가 없어 이동 시간을 계산하지 못했어요.", slotId);
    }
}
