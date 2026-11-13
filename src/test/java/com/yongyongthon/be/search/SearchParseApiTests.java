package com.yongyongthon.be.search;

import com.yongyongthon.be.common.ai.OpenAiClient;
import com.yongyongthon.be.common.ai.exception.AiServerException;
import com.yongyongthon.be.common.ai.exception.AiTimeoutException;
import com.yongyongthon.be.common.response.GlobalExceptionHandler;
import com.yongyongthon.be.common.response.PublicApiExceptionHandler;
import com.yongyongthon.be.search.controller.SearchController;
import com.yongyongthon.be.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DB와 OpenAI 없이 E4를 확인한다. OpenAiClient는 목으로 바꾸고, 모델 출력을 정리하는 규칙을 검사한다.
 */
class SearchParseApiTests {

    private final OpenAiClient openAiClient = mock(OpenAiClient.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SearchController(new SearchService(openAiClient, JsonMapper.builder().build(), 15)))
                .setControllerAdvice(new PublicApiExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    private void modelReturns(String json) {
        when(openAiClient.completeJson(anyString(), anyString(), any())).thenReturn(json);
    }

    private ResultActions parse(String body) throws Exception {
        return mockMvc.perform(post("/api/search/parse").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void 뽑은_조건은_그대로_주고_못뽑은_조건은_unresolved() throws Exception {
        modelReturns("""
                {"dateFrom":"2026-10-17","dateTo":"2026-10-18","district":["기흥구"],"dong":["마북동"],
                 "companion":"kids","kidsAge":5,"budget":"free","indoorOutdoor":null}
                """);

        parse("{\"query\":\"이번 주말 마북동에서 5살 아이랑 무료\",\"today\":\"2026-10-15\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parsed").value(true))
                .andExpect(jsonPath("$.data.conditions.dateFrom").value("2026-10-17"))
                .andExpect(jsonPath("$.data.conditions.dateTo").value("2026-10-18"))
                .andExpect(jsonPath("$.data.conditions.district").value(contains("기흥구")))
                .andExpect(jsonPath("$.data.conditions.dong").value(contains("마북동")))
                .andExpect(jsonPath("$.data.conditions.companion").value("kids"))
                .andExpect(jsonPath("$.data.conditions.kidsAge").value(5))
                .andExpect(jsonPath("$.data.conditions.budget").value("free"))
                .andExpect(jsonPath("$.data.conditions.indoorOutdoor").value(nullValue()))
                .andExpect(jsonPath("$.data.unresolved").value(contains("indoorOutdoor")))
                .andExpect(jsonPath("$.data.message").value(nullValue()));
    }

    @Test
    void 기준일과_요일을_프롬프트에_넣고_타임아웃은_설정값() throws Exception {
        modelReturns("{}");

        parse("{\"query\":\"이번 주말\",\"today\":\"2026-10-17\"}");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(openAiClient).completeJson(prompt.capture(), eq("이번 주말"), eq(Duration.ofSeconds(15)));
        assertThat(prompt.getValue()).contains("2026-10-17(토요일)").contains("이번주말: 2026-10-17 ~ 2026-10-18");
    }

    @Test
    void 허용되지_않는_값은_버린다() throws Exception {
        modelReturns("""
                {"dateFrom":"10월 17일","dateTo":"2026-10-18","district":["강남구","수지구","수지구"],
                 "dong":["역삼"],"companion":"pets","kidsAge":-3,"budget":"cheap","indoorOutdoor":"both"}
                """);

        parse("{\"query\":\"아무 문장\"}")
                .andExpect(jsonPath("$.data.parsed").value(true))
                .andExpect(jsonPath("$.data.conditions.dateFrom").value(nullValue()))
                .andExpect(jsonPath("$.data.conditions.dateTo").value("2026-10-18"))
                // 문장에 구 이름이 없으면 모델이 낸 구는 버린다
                .andExpect(jsonPath("$.data.conditions.district").value(nullValue()))
                .andExpect(jsonPath("$.data.conditions.dong").value(nullValue()))
                .andExpect(jsonPath("$.data.conditions.companion").value(nullValue()))
                .andExpect(jsonPath("$.data.conditions.kidsAge").value(nullValue()))
                .andExpect(jsonPath("$.data.conditions.budget").value(nullValue()))
                .andExpect(jsonPath("$.data.conditions.indoorOutdoor").value(nullValue()));
    }

    @Test
    void 동의_소속_구는_서버_표로_채우고_모델이_틀린_구는_버린다() throws Exception {
        // 실제 OpenAI가 마북동을 처인구로 낸 사례 (2026-11-14)
        modelReturns("{\"district\":[\"처인구\"],\"dong\":[\"마북동\"]}");

        parse("{\"query\":\"마북동에서 아이랑\",\"today\":\"2026-11-14\"}")
                .andExpect(jsonPath("$.data.conditions.district").value(contains("기흥구")))
                .andExpect(jsonPath("$.data.conditions.dong").value(contains("마북동")));
    }

    @Test
    void 구는_구_없이_말해도_인정하고_읍면은_처인구() throws Exception {
        modelReturns("{\"district\":[\"수지구\"]}");
        parse("{\"query\":\"수지 쪽 공연\"}")
                .andExpect(jsonPath("$.data.conditions.district").value(contains("수지구")));

        modelReturns("{}");
        parse("{\"query\":\"백암 순대 축제\"}")
                .andExpect(jsonPath("$.data.conditions.district").value(contains("처인구")))
                .andExpect(jsonPath("$.data.conditions.dong").value(nullValue()));
    }

    @Test
    void 흔한_낱말은_지명으로_읽지_않는다() throws Exception {
        modelReturns("{}");

        parse("{\"query\":\"이동 거리 짧은 가족 구성 맞춤 행사\"}")
                .andExpect(jsonPath("$.data.conditions.district").value(nullValue()));
    }

    @Test
    void 상대_날짜는_서버가_계산한_값으로_덮어쓴다() throws Exception {
        // 실제 OpenAI가 토요일 기준 이번 주말과 다음 주를 모두 오늘 하루로 낸 사례 (2026-11-14 토)
        modelReturns("{\"dateFrom\":\"2026-11-14\",\"dateTo\":\"2026-11-14\"}");

        parse("{\"query\":\"이번 주말\",\"today\":\"2026-11-14\"}")
                .andExpect(jsonPath("$.data.conditions.dateFrom").value("2026-11-14"))
                .andExpect(jsonPath("$.data.conditions.dateTo").value("2026-11-15"));
        parse("{\"query\":\"다음주 공연\",\"today\":\"2026-11-14\"}")
                .andExpect(jsonPath("$.data.conditions.dateFrom").value("2026-11-16"))
                .andExpect(jsonPath("$.data.conditions.dateTo").value("2026-11-22"));
        parse("{\"query\":\"다음 주말\",\"today\":\"2026-11-14\"}")
                .andExpect(jsonPath("$.data.conditions.dateFrom").value("2026-11-21"))
                .andExpect(jsonPath("$.data.conditions.dateTo").value("2026-11-22"));
        // 평일 기준 이번 주말과 일요일 기준 이번 주말
        parse("{\"query\":\"주말에\",\"today\":\"2026-11-12\"}")
                .andExpect(jsonPath("$.data.conditions.dateFrom").value("2026-11-14"))
                .andExpect(jsonPath("$.data.conditions.dateTo").value("2026-11-15"));
        parse("{\"query\":\"이번 주말\",\"today\":\"2026-11-15\"}")
                .andExpect(jsonPath("$.data.conditions.dateFrom").value("2026-11-15"))
                .andExpect(jsonPath("$.data.conditions.dateTo").value("2026-11-15"));
    }

    @Test
    void 요일이_있으면_그_요일로_계산한다() throws Exception {
        // 실제 사례: "다음 주 토요일"이 다음 주 전체(11-16 ~ 11-22)로 덮어써졌다 (2026-11-14 토 기준)
        modelReturns("{\"dateFrom\":\"2026-11-21\",\"dateTo\":\"2026-11-21\"}");
        expectDates("다음 주 토요일 아이랑", "2026-11-14", "2026-11-21", "2026-11-21");
        expectDates("다음주 토요일 일요일", "2026-11-14", "2026-11-21", "2026-11-22");
        expectDates("이번 주 금요일", "2026-11-12", "2026-11-13", "2026-11-13");
        expectDates("금주 일요일", "2026-11-12", "2026-11-15", "2026-11-15");
        // 주 표현이 없으면 오늘을 포함해 가장 가까운 그 요일
        expectDates("토요일에 갈 곳", "2026-11-14", "2026-11-14", "2026-11-14");
        expectDates("월요일 공연", "2026-11-14", "2026-11-16", "2026-11-16");
        // 요일 없이 주만 말하면 그 주 전체. 주말 표현에 요일이 붙으면 그 요일 하루
        expectDates("다음 주 공연", "2026-11-14", "2026-11-16", "2026-11-22");
        expectDates("다음 주말 토요일", "2026-11-14", "2026-11-21", "2026-11-21");
        expectDates("이번 주말 일요일", "2026-11-14", "2026-11-15", "2026-11-15");
    }

    private void expectDates(String query, String today, String from, String to) throws Exception {
        parse("{\"query\":\"" + query + "\",\"today\":\"" + today + "\"}")
                .andExpect(jsonPath("$.data.conditions.dateFrom").value(from))
                .andExpect(jsonPath("$.data.conditions.dateTo").value(to));
    }

    @Test
    void 기간이_역전되면_날짜를_모두_버린다() throws Exception {
        modelReturns("{\"dateFrom\":\"2026-10-18\",\"dateTo\":\"2026-10-17\",\"budget\":\"free\"}");

        parse("{\"query\":\"아무 문장\"}")
                .andExpect(jsonPath("$.data.conditions.dateFrom").value(nullValue()))
                .andExpect(jsonPath("$.data.conditions.dateTo").value(nullValue()));
    }

    @Test
    void 하나도_못뽑으면_parsed_false와_안내문구이고_200() throws Exception {
        modelReturns("{\"dateFrom\":null,\"district\":[],\"companion\":\"unknown\"}");

        parse("{\"query\":\"아무거나\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parsed").value(false))
                .andExpect(jsonPath("$.data.conditions", hasKey("dateFrom")))
                .andExpect(jsonPath("$.data.conditions.district").value(nullValue()))
                .andExpect(jsonPath("$.data.unresolved").value(
                        containsInAnyOrder("dateFrom", "district", "companion", "budget", "indoorOutdoor")))
                .andExpect(jsonPath("$.data.message").value("조건을 이해하지 못했어요. 아래에서 직접 골라 주세요."));
    }

    @Test
    void 모델이_JSON이_아닌_값을_내면_E5021() throws Exception {
        modelReturns("죄송합니다");

        parse("{\"query\":\"이번 주말\"}")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("E5021"));
    }

    @Test
    void AI_호출_실패와_시간초과() throws Exception {
        when(openAiClient.completeJson(anyString(), anyString(), any())).thenThrow(new AiServerException());
        parse("{\"query\":\"이번 주말\"}")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("E5021"));

        // 이미 예외를 던지도록 스텁된 목이라 when(...) 대신 doThrow로 바꾼다
        doThrow(new AiTimeoutException()).when(openAiClient).completeJson(anyString(), anyString(), any());
        parse("{\"query\":\"이번 주말\"}")
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error.code").value("E5041"));
    }

    @Test
    void 요청값_오류는_E4001이고_AI를_부르지_않는다() throws Exception {
        for (String body : new String[]{
                "{}",
                "{\"query\":\"   \"}",
                "{\"query\":\"" + "가".repeat(201) + "\"}",
                "{\"query\":\"이번 주말\",\"today\":\"2026/10/17\"}"}) {
            parse(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("E4001"));
        }
        verify(openAiClient, never()).completeJson(anyString(), anyString(), any());
    }
}
