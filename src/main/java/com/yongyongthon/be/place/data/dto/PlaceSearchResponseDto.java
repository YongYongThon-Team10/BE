package com.yongyongthon.be.place.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * E14 GET /api/places/search 응답 (명세 8장의 제안 형태). 고른 값이 E5의 origin.address, lat, lng가 된다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResponseDto {

    private List<Place> places;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Place {
        private String name;
        private String address;
        private Double lat;
        private Double lng;
    }
}
