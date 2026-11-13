package com.yongyongthon.be.itinerary.controller;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.swagger.ApiErrorCodes;
import com.yongyongthon.be.itinerary.data.dto.ItineraryRequestDto;
import com.yongyongthon.be.itinerary.data.dto.ItineraryResponseDto;
import com.yongyongthon.be.itinerary.data.dto.ShareResponseDto;
import com.yongyongthon.be.itinerary.service.ItineraryService;
import com.yongyongthon.be.itinerary.service.ItineraryShareService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 하루 일정 (S5 #1, #2, #9, #10). 로그인 없이 쓰는 데모 API라 인증 헤더를 읽지 않는다.
 */
@RestController
@RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
public class ItineraryController {
    private final ItineraryService itineraryService;
    private final ItineraryShareService itineraryShareService;

    @PostMapping
    @Operation(summary = "하루 일정 생성")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.EVENT_NOT_FOUND})
    public GlobalResponse<ItineraryResponseDto> createItinerary(
            @RequestBody(required = false) ItineraryRequestDto request) {
        return itineraryService.createItinerary(request);
    }

    @GetMapping("/{itineraryId}")
    @Operation(summary = "일정 조회")
    @ApiErrorCodes({ErrorCode.ITINERARY_NOT_FOUND})
    public GlobalResponse<ItineraryResponseDto> getItinerary(@PathVariable String itineraryId) {
        return itineraryService.getItinerary(itineraryId);
    }

    @PostMapping("/{itineraryId}/regenerate")
    @Operation(summary = "조건 바꿔 재추천")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.EVENT_NOT_FOUND, ErrorCode.ITINERARY_NOT_FOUND})
    public GlobalResponse<ItineraryResponseDto> regenerateItinerary(
            @PathVariable String itineraryId,
            @RequestBody(required = false) ItineraryRequestDto request) {
        return itineraryService.regenerateItinerary(itineraryId, request);
    }

    @PostMapping("/{itineraryId}/share")
    @Operation(summary = "공유 링크 만들기")
    @ApiErrorCodes({ErrorCode.ITINERARY_NOT_FOUND})
    public GlobalResponse<ShareResponseDto> shareItinerary(@PathVariable String itineraryId) {
        return itineraryShareService.shareItinerary(itineraryId);
    }
}
