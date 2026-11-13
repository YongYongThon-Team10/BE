package com.yongyongthon.be.place.service;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.place.data.dto.PlaceSearchResponseDto;
import com.yongyongthon.be.place.data.exception.PlaceSearchFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * E14 출발지 장소 검색. 카카오 로컬 키워드 검색을 그대로 감싼다 (결정 14).
 * 키 없음, 연결 실패, 오류 응답, 시간 초과는 모두 502 E5022.
 */
@Service
@Slf4j
public class PlaceService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RESULTS = 15;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public PlaceService(ObjectMapper objectMapper,
                        @Value("${kakao.rest-api-key:}") String apiKey,
                        @Value("${kakao.local-base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public GlobalResponse<PlaceSearchResponseDto> search(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("[search] KAKAO_REST_API_KEY가 설정되지 않음");
            throw new PlaceSearchFailedException();
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v2/local/search/keyword.json?size="
                        + MAX_RESULTS + "&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                .timeout(TIMEOUT)
                .header("Authorization", "KakaoAK " + apiKey)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // 시간 초과(HttpTimeoutException)도 여기로 온다
            log.error("[search] 장소 검색 서버 연결 실패: {}", e.getMessage());
            throw new PlaceSearchFailedException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlaceSearchFailedException();
        }

        if (response.statusCode() != 200) {
            log.error("[search] 장소 검색 오류 응답: {}", response.statusCode());
            throw new PlaceSearchFailedException();
        }

        List<PlaceSearchResponseDto.Place> places = new ArrayList<>();
        try {
            for (JsonNode document : objectMapper.readTree(response.body()).path("documents")) {
                String roadAddress = document.path("road_address_name").asString("");
                places.add(PlaceSearchResponseDto.Place.builder()
                        .name(document.path("place_name").asString())
                        // 도로명 주소가 없으면 지번 주소
                        .address(roadAddress.isBlank() ? document.path("address_name").asString() : roadAddress)
                        // 카카오는 좌표를 문자열로 준다. x가 경도, y가 위도
                        .lat(Double.valueOf(document.path("y").asString()))
                        .lng(Double.valueOf(document.path("x").asString()))
                        .build());
            }
        } catch (JacksonException | NumberFormatException e) {
            log.error("[search] 장소 검색 응답 형식 오류: {}", e.getMessage());
            throw new PlaceSearchFailedException();
        }

        log.info("[search] 장소 검색 완료: {}건", places.size());
        return GlobalResponse.ok(PlaceSearchResponseDto.builder().places(places).build());
    }
}
