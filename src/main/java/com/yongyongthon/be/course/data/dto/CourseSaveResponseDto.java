package com.yongyongthon.be.course.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 내 코스 저장 응답 (지시서 7장 #12).
 */
public record CourseSaveResponseDto(
        String courseId,
        String itineraryId,
        String title,
        @JsonProperty("isSaved") boolean isSaved,
        String savedAt
) {
}
