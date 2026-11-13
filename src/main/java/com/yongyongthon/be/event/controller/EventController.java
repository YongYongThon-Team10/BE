package com.yongyongthon.be.event.controller;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.security.SecurityUtil;
import com.yongyongthon.be.event.data.dto.EventDetailResponseDto;
import com.yongyongthon.be.event.data.dto.EventDiscrepancyResponseDto;
import com.yongyongthon.be.event.data.dto.EventListResponseDto;
import com.yongyongthon.be.event.data.dto.EventSearchRequestDto;
import com.yongyongthon.be.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    @Operation(summary = "행사 목록 및 지도 조회 (E1)")
    public GlobalResponse<EventListResponseDto> searchEvents(
            @ParameterObject @Valid @ModelAttribute EventSearchRequestDto request) {
        return eventService.searchEvents(request, SecurityUtil.getUsernameOrNull());
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "행사 상세 조회 (E2)")
    public GlobalResponse<EventDetailResponseDto> getEvent(@PathVariable String eventId) {
        return eventService.getEvent(eventId, SecurityUtil.getUsernameOrNull());
    }

    @GetMapping("/{eventId}/discrepancies")
    @Operation(summary = "행사 안내 상이 내용 조회 (E3)")
    public GlobalResponse<EventDiscrepancyResponseDto> getDiscrepancies(@PathVariable String eventId) {
        return eventService.getDiscrepancies(eventId);
    }
}
