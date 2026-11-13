package com.yongyongthon.be.common.ai;

import com.yongyongthon.be.common.ai.exception.AiServerException;
import com.yongyongthon.be.common.ai.exception.AiTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions를 JSON 모드로 호출한다. 의존성을 늘리지 않으려고 JDK HttpClient를 쓴다.
 * 실패는 모두 502 E5021, 시간 초과는 504 E5041로 바꿔 던진다.
 */
@Slf4j
@Component
public class OpenAiClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public OpenAiClient(ObjectMapper objectMapper,
                        @Value("${openai.api-key:}") String apiKey,
                        @Value("${openai.model}") String model,
                        @Value("${openai.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    /**
     * @return 모델이 낸 JSON 문자열 (message.content)
     */
    public String completeJson(String systemPrompt, String userPrompt, Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("[completeJson] OPENAI_API_KEY가 설정되지 않음");
            throw new AiServerException();
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            log.error("[completeJson] AI 응답 시간 초과: {}초", timeout.toSeconds());
            throw new AiTimeoutException();
        } catch (IOException e) {
            log.error("[completeJson] AI 서버 연결 실패: {}", e.getMessage());
            throw new AiServerException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServerException();
        }

        if (response.statusCode() != 200) {
            log.error("[completeJson] AI 서버 오류 응답: {}", response.statusCode());
            throw new AiServerException();
        }

        try {
            String content = objectMapper.readTree(response.body())
                    .path("choices").path(0).path("message").path("content").asString();
            if (content == null || content.isBlank()) {
                throw new AiServerException();
            }
            return content;
        } catch (JacksonException e) {
            log.error("[completeJson] AI 응답 형식 오류: {}", e.getMessage());
            throw new AiServerException();
        }
    }
}
