package com.yongyongthon.be.itinerary.controller;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.swagger.ApiErrorCodes;
import com.yongyongthon.be.itinerary.data.dto.SharedItineraryResponseDto;
import com.yongyongthon.be.itinerary.service.ItineraryShareService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공유 일정 미리보기 (S5 #11). 읽기 전용.
 */
@RestController
@RequestMapping("/api/shared")
@RequiredArgsConstructor
public class SharedItineraryController {
    private final ItineraryShareService itineraryShareService;

    @GetMapping("/{shareId}")
    @Operation(summary = "공유 일정 미리보기")
    @ApiErrorCodes({ErrorCode.SHARED_ITINERARY_NOT_FOUND})
    public GlobalResponse<SharedItineraryResponseDto> getSharedItinerary(@PathVariable String shareId) {
        return itineraryShareService.getSharedItinerary(shareId);
    }
}
