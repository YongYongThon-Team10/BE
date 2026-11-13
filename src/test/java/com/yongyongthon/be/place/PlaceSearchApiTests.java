package com.yongyongthon.be.place;

import com.sun.net.httpserver.HttpServer;
import com.yongyongthon.be.common.response.GlobalExceptionHandler;
import com.yongyongthon.be.common.response.PublicApiExceptionHandler;
import com.yongyongthon.be.place.controller.PlaceController;
import com.yongyongthon.be.place.service.PlaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 카카오 대신 로컬 HTTP 서버로 E14의 요청 형태, 응답 변환, 오류 코드를 확인한다.
 */
class PlaceSearchApiTests {

    private HttpServer kakao;
    private final AtomicReference<String> receivedQuery = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody = "{\"documents\":[]}";

    @BeforeEach
    void start() throws IOException {
        kakao = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        kakao.createContext("/v2/local/search/keyword.json", exchange -> {
            receivedQuery.set(exchange.getRequestURI().getQuery());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        kakao.start();
    }

    @AfterEach
    void stop() {
        kakao.stop(0);
    }

    private MockMvc mockMvc(String apiKey) {
        PlaceService service = new PlaceService(JsonMapper.builder().build(), apiKey,
                "http://127.0.0.1:" + kakao.getAddress().getPort());
        return MockMvcBuilders.standaloneSetup(new PlaceController(service))
                .setControllerAdvice(new PublicApiExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 카카오_결과를_이름_주소_좌표로_바꾼다() throws Exception {
        responseBody = """
                {"documents":[
                  {"place_name":"기흥역 수인분당선","road_address_name":"경기 용인시 기흥구 구갈로 지하 1",
                   "address_name":"경기 용인시 기흥구 구갈동 657","x":"127.115","y":"37.275"},
                  {"place_name":"기흥역 공영주차장","road_address_name":"",
                   "address_name":"경기 용인시 기흥구 구갈동 600","x":"127.116","y":"37.276"}
                ]}
                """;

        mockMvc("kakao-key").perform(get("/api/places/search").param("query", " 기흥역 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places[0].name").value("기흥역 수인분당선"))
                .andExpect(jsonPath("$.data.places[0].address").value("경기 용인시 기흥구 구갈로 지하 1"))
                .andExpect(jsonPath("$.data.places[0].lat").value(37.275))
                .andExpect(jsonPath("$.data.places[0].lng").value(127.115))
                .andExpect(jsonPath("$.data.places[1].address").value("경기 용인시 기흥구 구갈동 600"));

        assertThat(authorization.get()).isEqualTo("KakaoAK kakao-key");
        assertThat(receivedQuery.get()).contains("query=기흥역").contains("size=15");
    }

    @Test
    void 결과가_없으면_빈_배열() throws Exception {
        mockMvc("kakao-key").perform(get("/api/places/search").param("query", "없는장소"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places").isEmpty());
    }

    @Test
    void 키가_없거나_카카오가_실패하면_E5022() throws Exception {
        mockMvc("").perform(get("/api/places/search").param("query", "기흥역"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("E5022"));
        assertThat(receivedQuery.get()).isNull();

        status = 401;
        responseBody = "{\"errorType\":\"AccessDeniedError\"}";
        mockMvc("bad-key").perform(get("/api/places/search").param("query", "기흥역"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("E5022"));
    }

    @Test
    void 검색어_오류는_E4001() throws Exception {
        MockMvc mockMvc = mockMvc("kakao-key");
        for (var request : new org.springframework.test.web.servlet.RequestBuilder[]{
                get("/api/places/search"),
                get("/api/places/search").param("query", "   "),
                get("/api/places/search").param("query", "가".repeat(101))}) {
            mockMvc.perform(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("E4001"));
        }
        assertThat(receivedQuery.get()).isNull();
    }
}
