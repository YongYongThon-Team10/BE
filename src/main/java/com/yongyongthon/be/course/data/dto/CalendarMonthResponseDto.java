package com.yongyongthon.be.course.data.dto;

import java.util.List;

/**
 * 캘린더 월 조회 (지시서 7장 #15, month 모드).
 */
public record CalendarMonthResponseDto(
        String month,
        List<MonthEvent> events,
        List<MonthCourse> courses
) {

    public record MonthEvent(String eventId, String name, String startDate, String endDate) {
    }

    public record MonthCourse(String itineraryId, String title, String date) {
    }
}
