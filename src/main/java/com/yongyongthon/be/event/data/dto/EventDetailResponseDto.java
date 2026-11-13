package com.yongyongthon.be.event.data.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * E2 GET /api/events/{eventId} 응답. 명세 5장의 필드를 그대로 따른다.
 * 값이 없으면 키를 빼지 않고 null로 내보낸다. boolean 필드는 JSON 키가 isXxx로 나오도록 Boolean으로 둔다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailResponseDto {

    private String eventId;
    private String name;
    private String summary;
    private String category;
    private String subCategory;
    private String district;
    private String indoorOutdoor;
    private String imageUrl;
    private List<String> tags;
    private Boolean hasDiscrepancy;
    private Boolean isLiked;
    private KeyInfo keyInfo;
    private String notice;
    private Apply apply;
    private List<Program> programs;
    private List<Source> sources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyInfo {
        private LocalDate startDate;
        private LocalDate endDate;
        private String dateStatus;
        private String time;
        private Integer runningTimeMin;
        private String placeName;
        private String address;
        private String district;
        private String indoorOutdoor;
        private Double lat;
        private Double lng;
        private String coordPrecision;
        private String fee;
        private Boolean isFree;
        private String target;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Apply {
        private String type;
        private String text;
        private String channelType;
        private String label;
        private String url;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime receiptStart;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime receiptEnd;
        private String status;
    }

    // 프로그램 데이터가 아직 없어 항상 빈 배열이다. 데이터가 생기면 채운다
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Program {
        private String programId;
        private String type;
        private String name;
        private String time;
        private Integer durationMin;
        private Integer costAmount;
        private String costText;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String name;
        private String type;
        private String url;
    }
}
