package com.yongyongthon.be.course.data.dto;

/**
 * 캘린더 응답의 Swagger 문서 전용 모양. 실제 응답은 GlobalResponse로 나가고 모양은 같다.
 * month 조회와 date 조회의 data 모양이 달라서 둘 다 보여 주려고 둔다.
 */
public final class CalendarResponseDocs {

    private CalendarResponseDocs() {
    }

    public record ErrorBody(String code, String message) {
    }

    /** month 조회 응답 */
    public record MonthResponse(CalendarMonthResponseDto data, ErrorBody error) {
    }

    /** date 조회 응답 */
    public record DateResponse(CalendarDateResponseDto data, ErrorBody error) {
    }
}
