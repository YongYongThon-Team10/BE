package com.yongyongthon.be.itinerary.data.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * E5 POST /api/itineraries 요청 본문 (명세 7장, S4-8과 S5-1이 일치).
 * 신형 담당은 이 DTO와 ItineraryRequestValidator까지다. 컨트롤러, 일정 생성, 응답은 S5 담당이 만든다.
 * 컨트롤러에서 @Valid @RequestBody로 받으면 애너테이션 검증 실패가 E4001로 응답된다 (itinerary 패키지).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryCreateRequestDto {

    @NotBlank
    private String baseEventId;

    @NotNull
    private LocalDate date;

    // 생략하면 상관없음
    @Pattern(regexp = "alone|couple|friends|family|kids")
    private String companion;

    // companion=kids일 때만 의미가 있다. 다른 동행이면 무시한다
    @Min(0)
    private Integer kidsAge;

    @NotNull
    @Valid
    private Origin origin;

    @NotNull
    @Pattern(regexp = "car|transit|walk")
    private String transport;

    @NotNull
    @Valid
    private TimeWindow timeWindow;

    @Pattern(regexp = "any|free")
    private String budget;

    private List<@Pattern(regexp = "food|experience|rest|performance") String> wants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Origin {
        @NotNull
        @Pattern(regexp = "current|manual")
        private String type;

        // 직접 입력은 장소 검색(E14) 결과를 골라야 좌표가 생긴다
        @NotNull
        @DecimalMin("-90") @DecimalMax("90")
        private Double lat;

        @NotNull
        @DecimalMin("-180") @DecimalMax("180")
        private Double lng;

        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeWindow {
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        private LocalTime start;

        // start보다 늦어야 한다 (ItineraryRequestValidator가 검사)
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        private LocalTime end;
    }
}
