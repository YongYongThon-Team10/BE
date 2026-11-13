package com.yongyongthon.be.search.controller;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.search.data.dto.SearchParseRequestDto;
import com.yongyongthon.be.search.data.dto.SearchParseResponseDto;
import com.yongyongthon.be.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @PostMapping("/parse")
    @Operation(summary = "자연어 문장을 검색 조건으로 변환 (E4)")
    public GlobalResponse<SearchParseResponseDto> parse(@Valid @RequestBody SearchParseRequestDto request) {
        return searchService.parse(request);
    }
}
