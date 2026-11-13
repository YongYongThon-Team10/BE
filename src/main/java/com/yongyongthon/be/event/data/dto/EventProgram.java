package com.yongyongthon.be.event.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 행사 안의 프로그램 (행사 상세의 programs[]).
 * 지금 행사 데이터(용인시 엑셀)에는 프로그램이 없어서 항상 빈 목록이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventProgram(
        String programId,
        String name,
        Integer durationMin,
        String fee,
        @JsonProperty("isFree") Boolean isFree,
        String summary
) {
}
