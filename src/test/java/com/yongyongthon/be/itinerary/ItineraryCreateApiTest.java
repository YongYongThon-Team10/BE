package com.yongyongthon.be.itinerary;

import com.jayway.jsonpath.JsonPath;
import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 9장 4단계: #1 일정 생성, #2 일정 조회 (10-1 시나리오).
 */
class ItineraryCreateApiTest extends ApiTestSupport {

    @Test
    void 정상_생성_슬롯_순서와_비용() throws Exception {
        postJson(ITINERARIES, createBody("T-BASE", "2026-11-14", "10:00", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(nullValue()))
                .andExpect(jsonPath("$.data.title").value("호수공원 가을 산책마켓 중심 하루"))
                .andExpect(jsonPath("$.data.baseEventId").value("T-BASE"))
                .andExpect(jsonPath("$.data.timeWindow.start").value("10:00"))
                .andExpect(jsonPath("$.data.isSaved").value(false))
                .andExpect(jsonPath("$.data.crowdLevel").value(nullValue()))
                .andExpect(jsonPath("$.data.crowdLabel").value(nullValue()))
                .andExpect(jsonPath("$.data.slots", hasSize(5)))
                .andExpect(jsonPath("$.data.slots[0].type").value("event"))
                .andExpect(jsonPath("$.data.slots[0].order").value(1))
                .andExpect(jsonPath("$.data.slots[0].slotId").value("slt_1"))
                .andExpect(jsonPath("$.data.slots[0].startTime").value("10:05"))
                .andExpect(jsonPath("$.data.slots[0].endTime").value("11:05"))
                .andExpect(jsonPath("$.data.slots[0].timeBasis").value("official"))
                .andExpect(jsonPath("$.data.slots[0].costAmount").value(0))
                .andExpect(jsonPath("$.data.slots[0].costText").value("무료"))
                .andExpect(jsonPath("$.data.slots[0].description").value("호숫가 마켓"))
                .andExpect(jsonPath("$.data.slots[0].moveFromPrev.isEstimate").value(true))
                .andExpect(jsonPath("$.data.slots[1].type").value("meal"))
                .andExpect(jsonPath("$.data.slots[1].title").value("식사"))
                .andExpect(jsonPath("$.data.slots[1].place").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[1].costAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[1].isUserAdded").value(false))
                .andExpect(jsonPath("$.data.slots[2].type").value("nearbyEvent"))
                .andExpect(jsonPath("$.data.slots[2].eventId").value("T-NEAR1"))
                .andExpect(jsonPath("$.data.slots[2].costAmount").value(5000))
                .andExpect(jsonPath("$.data.slots[3].eventId").value("T-NEAR2"))
                .andExpect(jsonPath("$.data.slots[3].costAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[4].type").value("return"))
                .andExpect(jsonPath("$.data.slots[4].order").value(5))
                .andExpect(jsonPath("$.data.slots[4].title").value("집으로 돌아가기"))
                .andExpect(jsonPath("$.data.slots[4].place.lat").value(37.2756))
                .andExpect(jsonPath("$.data.slots[4].costAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.totalCost").value(5000))
                .andExpect(jsonPath("$.data.costUnknownCount").value(2))
                .andExpect(jsonPath("$.data.warnings", hasSize(1)))
                .andExpect(jsonPath("$.data.warnings[0].code").value("TIME_UNKNOWN"))
                .andExpect(jsonPath("$.data.warnings[0].slotId").value("slt_3"));
    }

    @Test
    void 근처_행사가_없으면_빈_슬롯과_NO_NEARBY_EVENT() throws Exception {
        postJson(ITINERARIES, createBody("T-ALONE", "2026-12-10", "10:00", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots", hasSize(4)))
                .andExpect(jsonPath("$.data.slots[2].type").value("nearbyEvent"))
                .andExpect(jsonPath("$.data.slots[2].title").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[2].eventId").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[2].place").value(nullValue()))
                .andExpect(jsonPath("$.data.warnings[0].code").value("NO_NEARBY_EVENT"))
                .andExpect(jsonPath("$.data.warnings[0].slotId").value("slt_3"));
    }

    @Test
    void 짧은_시간대는_자르지_않고_TIME_EXCEEDED() throws Exception {
        postJson(ITINERARIES, createBody("T-BASE", "2026-11-14", "10:00", "10:30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[0].durationMin").value(60))
                .andExpect(jsonPath("$.data.slots[0].endTime").value("11:05"))
                .andExpect(jsonPath("$.data.warnings[0].code").value("TIME_EXCEEDED"))
                .andExpect(jsonPath("$.data.warnings[0].message").value("설정한 시간대(10:00~10:30)를 넘었어요."))
                .andExpect(jsonPath("$.data.warnings[0].slotId").value("slt_4"));
    }

    @Test
    void 기준_행사에_시간이_없으면_TIME_UNKNOWN과_suggested() throws Exception {
        postJson(ITINERARIES, createBody("T-NOTIME", "2026-11-14", "10:00", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[0].timeBasis").value("suggested"))
                .andExpect(jsonPath("$.data.slots[0].costAmount").value(10000))
                .andExpect(jsonPath("$.data.slots[0].costText").value("유료 / 전석 1만원"))
                .andExpect(jsonPath("$.data.warnings[0].code").value("TIME_UNKNOWN"))
                .andExpect(jsonPath("$.data.warnings[0].slotId").value("slt_1"));
    }

    @Test
    void 좌표가_없는_행사는_moveFromPrev_null과_COORD_MISSING() throws Exception {
        String json = postJson(ITINERARIES, createBody("T-NOCOORD", "2026-11-14", "10:00", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[0].moveFromPrev").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[0].place.lat").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        java.util.List<String> codes = JsonPath.read(json, "$.data.warnings[*].code");
        org.assertj.core.api.Assertions.assertThat(codes).contains("COORD_MISSING", "NO_NEARBY_EVENT", "TIME_UNKNOWN");
    }

    @Test
    void budget_free면_무료_행사만_후보() throws Exception {
        String body = createBody("T-BASE", "2026-11-14", "10:00", "18:00").replace("\"any\"", "\"free\"");
        postJson(ITINERARIES, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[2].eventId").value("T-NEAR3"))
                .andExpect(jsonPath("$.data.slots[2].durationMin").value(90));
    }

    @Test
    void 종료가_시작보다_빠르면_C001() throws Exception {
        postJson(ITINERARIES, createBody("T-BASE", "2026-11-14", "18:00", "10:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 출발지_좌표_누락은_C001() throws Exception {
        String body = createBody("T-BASE", "2026-11-14", "10:00", "18:00").replace("\"lat\": 37.2756, ", "");
        postJson(ITINERARIES, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 잘못된_JSON은_C001() throws Exception {
        postJson(ITINERARIES, "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 날짜가_기준_행사_기간_밖이면_C001() throws Exception {
        postJson(ITINERARIES, createBody("T-BASE", "2026-12-14", "10:00", "18:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 없는_기준_행사는_404_C4042() throws Exception {
        postJson(ITINERARIES, createBody("NO-SUCH", "2026-11-14", "10:00", "18:00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4042"))
                .andExpect(jsonPath("$.error.message").value("행사를 찾을 수 없습니다."));
    }

    @Test
    void 생성한_일정을_같은_내용으로_조회() throws Exception {
        String created = postJson(ITINERARIES, createBody("T-BASE", "2026-11-14", "10:00", "18:00"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.data.itineraryId");

        String fetched = getUrl(ITINERARIES + "/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itineraryId").value(id))
                .andReturn().getResponse().getContentAsString();

        Object createdData = JsonPath.read(created, "$.data");
        Object fetchedData = JsonPath.read(fetched, "$.data");
        org.assertj.core.api.Assertions.assertThat(fetchedData).isEqualTo(createdData);
    }

    @Test
    void 없는_일정은_404_C4043() throws Exception {
        getUrl(ITINERARIES + "/itn_nope")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4043"));
    }

    @Test
    void Authorization_헤더가_있어도_무시() throws Exception {
        mvc.perform(post(ITINERARIES)
                        .header("Authorization", "Bearer not-a-real-token")
                        .contentType("application/json")
                        .content(createBody("T-BASE", "2026-11-14", "10:00", "18:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itineraryId").exists());
    }
}
