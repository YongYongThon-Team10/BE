package com.yongyongthon.be.course.controller;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.swagger.ApiErrorCodes;
import com.yongyongthon.be.course.data.dto.CalendarResponseDocs;
import com.yongyongthon.be.course.data.dto.CourseDeleteResponseDto;
import com.yongyongthon.be.course.data.dto.CourseListResponseDto;
import com.yongyongthon.be.course.data.dto.CourseSaveRequestDto;
import com.yongyongthon.be.course.data.dto.CourseSaveResponseDto;
import com.yongyongthon.be.course.service.CalendarService;
import com.yongyongthon.be.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 코스와 캘린더 (S5 #12, S6 #13, #14, #15).
 * "me"는 서버에 하나뿐인 데모 사용자다. 인증 헤더를 읽지 않는다.
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final CalendarService calendarService;

    @PostMapping("/courses")
    @Operation(summary = "내 코스에 저장")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.ITINERARY_NOT_FOUND})
    public GlobalResponse<CourseSaveResponseDto> saveCourse(
            @RequestBody(required = false) CourseSaveRequestDto request) {
        return courseService.saveCourse(request);
    }

    @GetMapping("/courses")
    @Operation(summary = "저장한 코스 목록")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST})
    public GlobalResponse<CourseListResponseDto> getCourses(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return courseService.getCourses(page, size);
    }

    @DeleteMapping("/courses/{courseId}")
    @Operation(summary = "저장한 코스 삭제")
    @ApiErrorCodes({ErrorCode.ITINERARY_NOT_FOUND})
    public GlobalResponse<CourseDeleteResponseDto> deleteCourse(@PathVariable String courseId) {
        return courseService.deleteCourse(courseId);
    }

    @GetMapping("/calendar")
    @Operation(summary = "행사 캘린더 (month 또는 date)")
    @ApiResponse(responseCode = "200", description = "month 조회면 MonthResponse, date 조회면 DateResponse 모양",
            content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {
                    CalendarResponseDocs.MonthResponse.class, CalendarResponseDocs.DateResponse.class})))
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST})
    public GlobalResponse<?> getCalendar(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String date) {
        return calendarService.getCalendar(month, date);
    }
}
