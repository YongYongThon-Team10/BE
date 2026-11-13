package com.yongyongthon.be.event.controller;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.security.SecurityUtil;
import com.yongyongthon.be.event.data.dto.FavoriteResponseDto;
import com.yongyongthon.be.event.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 로그인 필요. 경로가 permitAll 목록에 없으므로 토큰이 없으면 필터 단계에서 401 C4011
@RestController
@RequestMapping("/api/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PutMapping("/{eventId}")
    @Operation(summary = "관심 행사 담기 (E19)")
    public GlobalResponse<FavoriteResponseDto> addFavorite(@PathVariable String eventId) {
        return favoriteService.addFavorite(SecurityUtil.getUsername(), eventId);
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "관심 행사 해제 (E20)")
    public GlobalResponse<FavoriteResponseDto> removeFavorite(@PathVariable String eventId) {
        return favoriteService.removeFavorite(SecurityUtil.getUsername(), eventId);
    }
}
