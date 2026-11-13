package com.yongyongthon.be.course.data.dto;

import java.util.List;

/**
 * 캘린더 날짜 조회 (지시서 7장 #15, date 모드).
 */
public record CalendarDateResponseDto(
        String date,
        int eventCount,
        int courseCount,
        List<DateEvent> events,
        List<DateCourse> courses
) {

    public record DateEvent(String eventId, String name, String category, String startDate, String endDate) {
    }

    public record DateCourse(String itineraryId, String title, int slotCount) {
    }
}
