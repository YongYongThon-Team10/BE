package com.yongyongthon.be.search.service;

import com.yongyongthon.be.common.ai.OpenAiClient;
import com.yongyongthon.be.common.ai.exception.AiServerException;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.search.data.dto.SearchParseRequestDto;
import com.yongyongthon.be.search.data.dto.SearchParseResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * E4 자연어를 검색 조건으로 바꾼다. 모델 출력은 믿지 않고 허용된 값만 남긴다(나머지는 null).
 */
@Service
@Slf4j
public class SearchService {

    private static final Set<String> DISTRICTS = Set.of("처인구", "기흥구", "수지구");
    private static final Set<String> COMPANIONS = Set.of("alone", "couple", "friends", "family", "kids");
    private static final Set<String> BUDGETS = Set.of("any", "free");
    private static final Set<String> INDOOR_OUTDOOR = Set.of("indoor", "outdoor");
    private static final String FAILED_MESSAGE = "조건을 이해하지 못했어요. 아래에서 직접 골라 주세요.";

    private static final String SYSTEM_PROMPT = """
            너는 용인시 행사 검색 문장을 검색 조건 JSON으로 바꾼다. 기준일은 %s(%s)이다.
            아래 키만 가진 JSON 객체 하나를 출력한다. 문장에 없는 조건은 추측하지 말고 null로 둔다.
            1. dateFrom, dateTo: "yyyy-MM-dd". 상대 날짜는 기준일로 계산한다. 이번 주말은 다가오는 토요일과 일요일, \
            다음 주는 다음 주 월요일부터 일요일이다. 하루만 말하면 dateTo는 dateFrom과 같다.
            2. district: "처인구", "기흥구", "수지구" 중 언급된 구의 배열. 동만 말하면 그 동이 속한 구를 넣는다.
            3. dong: 언급된 용인시 동 이름의 배열 (예: "마북동").
            4. companion: alone, couple, friends, family, kids 중 하나. 아이와 함께면 kids.
            5. kidsAge: 아이 나이 숫자.
            6. budget: 무료를 원하면 free, 상관없다고 하면 any.
            7. indoorOutdoor: indoor 또는 outdoor. "무료 야외"처럼 함께 말하면 budget과 indoorOutdoor로 나눠 담는다.
            """;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public SearchService(OpenAiClient openAiClient, ObjectMapper objectMapper,
                         @Value("${ai.search-parse-timeout-seconds}") long timeoutSeconds) {
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public GlobalResponse<SearchParseResponseDto> parse(SearchParseRequestDto request) {
        LocalDate today = request.getToday() != null ? request.getToday() : LocalDate.now();
        String systemPrompt = SYSTEM_PROMPT.formatted(today,
                today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN));

        JsonNode output;
        try {
            output = objectMapper.readTree(openAiClient.completeJson(systemPrompt, request.getQuery(), timeout));
        } catch (JacksonException e) {
            log.error("[parse] AI 응답이 JSON이 아님: {}", e.getMessage());
            throw new AiServerException();
        }

        SearchParseResponseDto.Conditions conditions = toConditions(output);
        Map<String, Object> checked = new LinkedHashMap<>();
        checked.put("dateFrom", conditions.getDateFrom());
        checked.put("district", conditions.getDistrict());
        checked.put("companion", conditions.getCompanion());
        checked.put("budget", conditions.getBudget());
        checked.put("indoorOutdoor", conditions.getIndoorOutdoor());
        List<String> unresolved = checked.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .toList();

        boolean parsed = conditions.getDateFrom() != null || conditions.getDateTo() != null
                || conditions.getDistrict() != null || conditions.getDong() != null
                || conditions.getCompanion() != null || conditions.getKidsAge() != null
                || conditions.getBudget() != null || conditions.getIndoorOutdoor() != null;
        log.info("[parse] 검색 조건 변환 완료: parsed={}, unresolved={}", parsed, unresolved);

        return GlobalResponse.ok(SearchParseResponseDto.builder()
                .parsed(parsed)
                .conditions(conditions)
                .unresolved(unresolved)
                .message(parsed ? null : FAILED_MESSAGE)
                .build());
    }

    private SearchParseResponseDto.Conditions toConditions(JsonNode output) {
        LocalDate dateFrom = date(output.path("dateFrom"));
        LocalDate dateTo = date(output.path("dateTo"));
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            dateFrom = null;
            dateTo = null;
        }
        JsonNode kidsAge = output.path("kidsAge");

        return SearchParseResponseDto.Conditions.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .district(strings(output.path("district"), DISTRICTS::contains))
                .dong(strings(output.path("dong"), value -> value.length() > 1 && value.endsWith("동")))
                .companion(oneOf(output.path("companion"), COMPANIONS))
                .kidsAge(kidsAge.isIntegralNumber() && kidsAge.asInt() >= 0 ? kidsAge.asInt() : null)
                .budget(oneOf(output.path("budget"), BUDGETS))
                .indoorOutdoor(oneOf(output.path("indoorOutdoor"), INDOOR_OUTDOOR))
                .build();
    }

    private LocalDate date(JsonNode node) {
        if (!node.isString()) {
            return null;
        }
        try {
            return LocalDate.parse(node.asString());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String oneOf(JsonNode node, Set<String> allowed) {
        return node.isString() && allowed.contains(node.asString()) ? node.asString() : null;
    }

    // 허용된 문자열만 순서대로, 중복 없이. 남는 게 없으면 null
    private List<String> strings(JsonNode node, java.util.function.Predicate<String> allowed) {
        if (!node.isArray()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isString() && allowed.test(item.asString()) && !values.contains(item.asString())) {
                values.add(item.asString());
            }
        }
        return values.isEmpty() ? null : values;
    }
}
