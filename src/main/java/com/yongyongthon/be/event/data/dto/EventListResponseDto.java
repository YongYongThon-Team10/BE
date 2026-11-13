package com.yongyongthon.be.event.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * E1 GET /api/events 응답. view=map이면 page는 0, size는 totalCount이고 events에 전체가 담긴다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventListResponseDto {

    private long totalCount;
    private int page;
    private int size;
    private List<Item> events;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String eventId;
        private String name;
        private String summary;
        private String category;
        private String subCategory;
        private String themeLabel;
        private LocalDate startDate;
        private LocalDate endDate;
        private String dateStatus;
        private String time;
        private String placeName;
        private String address;
        private String district;
        private String indoorOutdoor;
        private Double lat;
        private Double lng;
        private String coordPrecision;
        private String fee;
        private Boolean isFree;
        private String applyType;
        private String applyUrl;
        private List<String> tags;
        private String sourceUrl;
        private String thumbnailUrl;
        private Double distanceKm;
        private Boolean hasDiscrepancy;
        private Boolean isLiked;
    }
}
