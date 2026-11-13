package com.yongyongthon.be.event.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * E3 GET /api/events/{eventId}/discrepancies 응답.
 * 출처별 값 데이터가 아직 없어 항상 hasDiscrepancy=false, items=[]다. 데이터가 생기면 items를 채운다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDiscrepancyResponseDto {

    private String eventId;
    private Boolean hasDiscrepancy;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        // date, time, place, fee, apply
        private String field;
        private String label;
        private List<Value> values;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {
        private String sourceName;
        private String sourceUrl;
        private String value;
    }
}
