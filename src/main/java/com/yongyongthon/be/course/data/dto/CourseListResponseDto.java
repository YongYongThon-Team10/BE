package com.yongyongthon.be.course.data.dto;

import java.util.List;

/**
 * 저장한 코스 목록 (지시서 7장 #13).
 */
public record CourseListResponseDto(
        long totalCount,
        int page,
        int size,
        List<CourseItem> courses
) {

    public record CourseItem(
            String courseId,
            String itineraryId,
            String title,
            String date,
            String companion,
            String transport,
            int slotCount,
            String summary,
            String mealSummary,
            String coverImageUrl,
            String savedAt
    ) {
    }
}
