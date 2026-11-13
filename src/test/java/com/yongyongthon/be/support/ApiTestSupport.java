package com.yongyongthon.be.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 일정 API 통합 테스트 공통. local 설정 위에 test 프로필(H2, 테스트 행사 JSON)을 얹는다.
 * MariaDB, Redis 없이 돈다.
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
public abstract class ApiTestSupport {

    protected static final String ITINERARIES = "/api/v1/itineraries";

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions getUrl(String url) throws Exception {
        return mvc.perform(get(url));
    }

    protected ResultActions putJson(String url, String body) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions patchJson(String url, String body) throws Exception {
        return mvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions deleteUrl(String url) throws Exception {
        return mvc.perform(delete(url));
    }

    /** 일정을 하나 만들고 itineraryId를 돌려준다 */
    protected String createItinerary(String baseEventId, String date) throws Exception {
        String json = postJson(ITINERARIES, createBody(baseEventId, date, "10:00", "18:00"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.itineraryId");
    }

    /** 기본 생성 요청 본문. 바꿀 값만 넘긴다 */
    protected static String createBody(String baseEventId, String date, String start, String end) {
        return """
                {
                  "baseEventId": "%s",
                  "date": "%s",
                  "origin": {"type": "manual", "lat": 37.2756, "lng": 127.1163, "address": "기흥역"},
                  "transport": "car",
                  "timeWindow": {"start": "%s", "end": "%s"},
                  "companion": "kids",
                  "kidsAge": 6,
                  "budget": "any",
                  "wants": ["food"]
                }
                """.formatted(baseEventId, date, start, end);
    }
}
