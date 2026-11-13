package com.yongyongthon.be.search.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * E4 응답. parsed=false도 오류가 아닌 200이고, 그때 conditions는 전부 null이다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchParseResponseDto {

    private Boolean parsed;
    private Conditions conditions;
    private List<String> unresolved;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conditions {
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private List<String> district;
        private List<String> dong;
        private String companion;
        private Integer kidsAge;
        private String budget;
        private String indoorOutdoor;
    }
}
