package com.yongyongthon.be.itinerary.controller;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.swagger.ApiErrorCodes;
import com.yongyongthon.be.itinerary.data.dto.ItineraryResponseDto;
import com.yongyongthon.be.itinerary.data.dto.SlotAddRequestDto;
import com.yongyongthon.be.itinerary.data.dto.SlotCandidatesResponseDto;
import com.yongyongthon.be.itinerary.data.dto.SlotOrderRequestDto;
import com.yongyongthon.be.itinerary.data.dto.SlotReplaceRequestDto;
import com.yongyongthon.be.itinerary.service.ItineraryCandidateService;
import com.yongyongthon.be.itinerary.service.ItinerarySlotService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일정 항목 수정 (S5 #3, #4, #5, #8)과 교체 후보 조회 (#6, #7).
 * 수정 API의 응답은 재계산된 일정 전체.
 */
@RestController
@RequestMapping("/api/v1/itineraries/{itineraryId}/slots")
@RequiredArgsConstructor
public class ItinerarySlotController {
    private final ItinerarySlotService itinerarySlotService;
    private final ItineraryCandidateService itineraryCandidateService;

    @PostMapping
    @Operation(summary = "슬롯 추가 (프로그램, 식사, 행사, 직접 입력)")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.EVENT_NOT_FOUND, ErrorCode.ITINERARY_NOT_FOUND, ErrorCode.SLOT_NOT_FOUND})
    public GlobalResponse<ItineraryResponseDto> addSlot(
            @PathVariable String itineraryId,
            @RequestBody(required = false) SlotAddRequestDto request) {
        return itinerarySlotService.addSlot(itineraryId, request);
    }

    @DeleteMapping("/{slotId}")
    @Operation(summary = "슬롯 삭제")
    @ApiErrorCodes({ErrorCode.SLOT_NOT_DELETABLE, ErrorCode.ITINERARY_NOT_FOUND, ErrorCode.SLOT_NOT_FOUND})
    public GlobalResponse<ItineraryResponseDto> deleteSlot(
            @PathVariable String itineraryId, @PathVariable String slotId) {
        return itinerarySlotService.deleteSlot(itineraryId, slotId);
    }

    @PutMapping("/{slotId}")
    @Operation(summary = "슬롯 교체")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.EVENT_NOT_FOUND, ErrorCode.ITINERARY_NOT_FOUND, ErrorCode.SLOT_NOT_FOUND})
    public GlobalResponse<ItineraryResponseDto> replaceSlot(
            @PathVariable String itineraryId, @PathVariable String slotId,
            @RequestBody(required = false) SlotReplaceRequestDto request) {
        return itinerarySlotService.replaceSlot(itineraryId, slotId, request);
    }

    @PatchMapping("/order")
    @Operation(summary = "슬롯 순서 변경")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.ITINERARY_NOT_FOUND})
    public GlobalResponse<ItineraryResponseDto> reorderSlots(
            @PathVariable String itineraryId,
            @RequestBody(required = false) SlotOrderRequestDto request) {
        return itinerarySlotService.reorderSlots(itineraryId, request);
    }

    @GetMapping("/{slotId}/alternatives")
    @Operation(summary = "교체 후보 조회 (행사)")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.ITINERARY_NOT_FOUND, ErrorCode.SLOT_NOT_FOUND})
    public GlobalResponse<SlotCandidatesResponseDto> getAlternatives(
            @PathVariable String itineraryId, @PathVariable String slotId,
            @RequestParam(required = false) Integer limit) {
        return itineraryCandidateService.getAlternatives(itineraryId, slotId, limit);
    }

    @GetMapping("/{slotId}/meal-candidates")
    @Operation(summary = "식사 후보 조회 (샘플)")
    @ApiErrorCodes({ErrorCode.INVALID_REQUEST, ErrorCode.ITINERARY_NOT_FOUND, ErrorCode.SLOT_NOT_FOUND})
    public GlobalResponse<SlotCandidatesResponseDto> getMealCandidates(
            @PathVariable String itineraryId, @PathVariable String slotId,
            @RequestParam(required = false) Integer limit) {
        return itineraryCandidateService.getMealCandidates(itineraryId, slotId, limit);
    }
}
