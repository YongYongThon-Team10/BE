package com.yongyongthon.be.itinerary.data.dto;

import java.util.List;

/**
 * 슬롯 순서 변경 요청 (지시서 7장 #8). return 슬롯은 넣지 않는다.
 */
public record SlotOrderRequestDto(List<String> slotIds) {
}
