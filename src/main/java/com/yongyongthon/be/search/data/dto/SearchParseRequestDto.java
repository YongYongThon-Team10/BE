package com.yongyongthon.be.search.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * E4 POST /api/search/parse 요청. 검증 실패는 E4001.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchParseRequestDto {

    @NotBlank
    @Size(max = 200)
    private String query;

    // 상대 날짜("이번 주말") 해석 기준일. 생략하면 서버 날짜
    private LocalDate today;
}
