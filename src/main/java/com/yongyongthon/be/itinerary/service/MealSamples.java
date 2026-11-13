package com.yongyongthon.be.itinerary.service;

import java.util.List;
import java.util.Optional;

/**
 * 식사 후보 샘플 5개 (지시서 7장 #7). 주변 식당 데이터가 없어서 "가정한 후보"로 고정한다.
 */
public final class MealSamples {

    public static final String ID_PREFIX = "cnd_m";
    private static final String DESCRIPTION = "아이와 함께 먹기 좋은 메뉴를 가정한 곳이에요.";

    public record MealSample(String candidateId, String name, int durationMin, int costAmount,
                             String costText, double distanceKm, String description) {
    }

    public static final List<MealSample> ALL = List.of(
            new MealSample("cnd_m1", "물빛 국수집", 50, 9000, "9,000원", 0.7, DESCRIPTION),
            new MealSample("cnd_m2", "산책길 브런치", 55, 15000, "15,000원", 0.9, DESCRIPTION),
            new MealSample("cnd_m3", "호수 옆 작은 식탁", 45, 12000, "12,000원", 0.5, DESCRIPTION),
            new MealSample("cnd_m4", "마을 분식", 40, 6000, "6,000원", 1.1, DESCRIPTION),
            new MealSample("cnd_m5", "숲속 도시락 카페", 50, 11000, "11,000원", 1.3, DESCRIPTION)
    );

    private MealSamples() {
    }

    public static boolean isMealCandidateId(String candidateId) {
        return candidateId != null && candidateId.startsWith(ID_PREFIX);
    }

    public static Optional<MealSample> find(String candidateId) {
        return ALL.stream().filter(m -> m.candidateId().equals(candidateId)).findFirst();
    }
}
