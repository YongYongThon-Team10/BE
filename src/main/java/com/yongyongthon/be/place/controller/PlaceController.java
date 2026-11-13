package com.yongyongthon.be.place.controller;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.place.data.dto.PlaceSearchResponseDto;
import com.yongyongthon.be.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 공개 API. 비로그인 사용자도 일정 만들기(E5)를 쓰므로 출발지 검색도 막지 않는다
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {
    private final PlaceService placeService;

    @GetMapping("/search")
    @Operation(summary = "출발지 장소 및 역 검색 (E14)")
    public GlobalResponse<PlaceSearchResponseDto> search(@RequestParam @NotBlank @Size(max = 100) String query) {
        return placeService.search(query.strip());
    }
}
