package com.yongyongthon.be.event.data.dto;

/**
 * 근처 행사 후보와 기준점에서의 직선거리(km, 반올림 전).
 */
public record NearbyEvent(EventInfo event, double distanceKm) {
}
