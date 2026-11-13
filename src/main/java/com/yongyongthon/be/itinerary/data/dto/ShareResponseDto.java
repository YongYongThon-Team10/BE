package com.yongyongthon.be.itinerary.data.dto;

/**
 * 공유 링크 응답 (지시서 7장 #10).
 */
public record ShareResponseDto(String shareId, String shareUrl, String title) {
}
