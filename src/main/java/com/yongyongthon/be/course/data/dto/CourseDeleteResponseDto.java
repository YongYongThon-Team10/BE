package com.yongyongthon.be.course.data.dto;

/**
 * 코스 삭제 응답 (지시서 7장 #14).
 */
public record CourseDeleteResponseDto(String courseId, boolean deleted) {
}
