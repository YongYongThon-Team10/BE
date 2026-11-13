package com.yongyongthon.be.itinerary;

import com.jayway.jsonpath.JsonPath;
import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 데이터 확인 후 더한 규칙 (PROGRESS.md "지시서와 다르게 정한 것").
 * 1. 기준 행사의 공식 시작 시각보다 일찍 배치하지 않는다
 * 2. 같은 날, 같은 장소, 비슷한 이름의 행사는 근처 후보에서 뺀다
 * 3. 제외 분류(도서관 프로그램)와 제외 단어(모집)는 근처 후보에서 뺀다
 */
class NearbyFilterAndTimeApiTest extends ApiTestSupport {

    @Test
    void 기준_행사는_공식_시작_시각까지_기다린다() throws Exception {
        postJson(ITINERARIES, createBody("T-LATE", "2026-11-14", "10:00", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[0].type").value("event"))
                .andExpect(jsonPath("$.data.slots[0].startTime").value("13:00"))
                .andExpect(jsonPath("$.data.slots[0].endTime").value("14:00"))
                .andExpect(jsonPath("$.data.slots[0].timeBasis").value("official"))
                .andExpect(jsonPath("$.data.slots[1].startTime").value("14:00"))
                .andExpect(jsonPath("$.data.warnings", hasSize(1)))
                .andExpect(jsonPath("$.data.warnings[0].code").value("NO_NEARBY_EVENT"));
    }

    @Test
    void 이미_시작_시각이_지났으면_기다리지_않는다() throws Exception {
        postJson(ITINERARIES, createBody("T-LATE", "2026-11-14", "15:00", "20:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[0].startTime").value("15:50"))
                .andExpect(jsonPath("$.data.slots[0].timeBasis").value("suggested"));
    }

    @Test
    void 생성할_때_제외_분류_모집_같은_행사는_근처_후보가_아니다() throws Exception {
        String json = postJson(ITINERARIES, createBody("T-BASE", "2026-11-14", "10:00", "18:00"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> eventIds = JsonPath.read(json, "$.data.slots[*].eventId");
        assertThat(eventIds).doesNotContain("T-LIB", "T-RECRUIT", "T-SAMEDAY");
        assertThat(eventIds).contains("T-NEAR1", "T-NEAR2");
    }

    @Test
    void 교체_후보에도_같은_규칙() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        String json = getUrl(ITINERARIES + "/" + id + "/slots/slt_3/alternatives?limit=10")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> eventIds = JsonPath.read(json, "$.data.candidates[*].eventId");
        assertThat(eventIds).doesNotContain("T-LIB", "T-RECRUIT", "T-SAMEDAY");
        assertThat(eventIds).containsExactly("T-NEAR3", "T-NOTIME");
    }

    @Test
    void 사용자가_직접_고르면_제외_행사도_넣을_수_있다() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson(ITINERARIES + "/" + id + "/slots/slt_3", "{\"replaceWithEventId\": \"T-LIB\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[2].eventId").value("T-LIB"));
    }
}
