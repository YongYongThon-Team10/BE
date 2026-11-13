package com.yongyongthon.be.event.data.dto;

/**
 * 행사 안의 프로그램 (행사 상세의 programs[]).
 * 지금 행사 데이터에는 프로그램이 없어서 항상 빈 목록이다.
 */
public record EventProgram(
        String programId,
        String name,
        Integer durationMin,
        String fee,
        Boolean isFree,
        String summary
) {
}
