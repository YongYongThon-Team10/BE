package com.yongyongthon.be.course.data.dto;

/**
 * 내 코스 저장 요청 (지시서 7장 #12). title은 생략하면 일정 제목.
 */
public record CourseSaveRequestDto(String itineraryId, String title) {
}
