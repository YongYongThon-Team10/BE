package com.yongyongthon.be.itinerary;

import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 9장 6단계: #6 alternatives, #7 meal-candidates (10-1 시나리오).
 * 기본 일정(T-BASE, 2026-11-14): slt_1 event, slt_2 meal, slt_3 T-NEAR1, slt_4 T-NEAR2, slt_5 return
 */
class ItineraryCandidateApiTest extends ApiTestSupport {

    private String slot(String id, String slotId) {
        return ITINERARIES + "/" + id + "/slots/" + slotId;
    }

    @Test
    void 행사_교체_후보는_가까운_순_cnd_번호() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        getUrl(slot(id, "slt_3") + "/alternatives")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slotId").value("slt_3"))
                .andExpect(jsonPath("$.data.type").value("nearbyEvent"))
                .andExpect(jsonPath("$.data.candidates", hasSize(2)))
                .andExpect(jsonPath("$.data.candidates[0].candidateId").value("cnd_1"))
                .andExpect(jsonPath("$.data.candidates[0].kind").value("event"))
                .andExpect(jsonPath("$.data.candidates[0].eventId").value("T-NEAR3"))
                .andExpect(jsonPath("$.data.candidates[0].name").value("근처 무료 체험"))
                .andExpect(jsonPath("$.data.candidates[0].category").value("체험"))
                .andExpect(jsonPath("$.data.candidates[0].placeName").value("근처 체험관"))
                .andExpect(jsonPath("$.data.candidates[0].time").value("11:00 ~ 12:00"))
                .andExpect(jsonPath("$.data.candidates[0].durationMin").value(90))
                .andExpect(jsonPath("$.data.candidates[0].costAmount").value(0))
                .andExpect(jsonPath("$.data.candidates[0].costText").value("무료"))
                .andExpect(jsonPath("$.data.candidates[0].distanceKmFromPrev").value(1.7))
                .andExpect(jsonPath("$.data.candidates[0].estimatedMinutes").value(5))
                .andExpect(jsonPath("$.data.candidates[0].description").value("체험 설명"))
                .andExpect(jsonPath("$.data.candidates[0].source").value("event"))
                .andExpect(jsonPath("$.data.candidates[1].candidateId").value("cnd_2"))
                .andExpect(jsonPath("$.data.candidates[1].eventId").value("T-NOTIME"))
                .andExpect(jsonPath("$.data.candidates[1].description").value(nullValue()));
    }

    @Test
    void 후보_조회_후_cnd_1로_교체() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        getUrl(slot(id, "slt_3") + "/alternatives").andExpect(status().isOk());
        putJson(slot(id, "slt_3"), "{\"replaceWithCandidateId\": \"cnd_1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[2].slotId").value("slt_3"))
                .andExpect(jsonPath("$.data.slots[2].eventId").value("T-NEAR3"))
                .andExpect(jsonPath("$.data.slots[2].type").value("nearbyEvent"));
        // 슬롯이 바뀌면 이전 후보는 지워진다
        putJson(slot(id, "slt_3"), "{\"replaceWithCandidateId\": \"cnd_2\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4044"));
    }

    @Test
    void 다시_조회하면_이전_후보를_덮어쓴다() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        getUrl(slot(id, "slt_3") + "/alternatives");
        getUrl(slot(id, "slt_3") + "/alternatives?limit=1")
                .andExpect(jsonPath("$.data.candidates", hasSize(1)));
        putJson(slot(id, "slt_3"), "{\"replaceWithCandidateId\": \"cnd_2\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4044"));
    }

    @Test
    void 후보가_없으면_빈_배열() throws Exception {
        String id = createItinerary("T-ALONE", "2026-12-10");
        getUrl(slot(id, "slt_3") + "/alternatives")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates", hasSize(0)));
    }

    @Test
    void 행사_후보_조회_오류() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        getUrl(slot(id, "slt_2") + "/alternatives").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(slot(id, "slt_1") + "/alternatives").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(slot(id, "slt_5") + "/alternatives").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(slot(id, "slt_3") + "/alternatives?limit=11").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(slot(id, "slt_3") + "/alternatives?limit=0").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(slot(id, "slt_3") + "/alternatives?limit=abc").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(slot(id, "slt_99") + "/alternatives")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4044"));
        getUrl(slot("itn_nope", "slt_3") + "/alternatives")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4043"));
    }

    @Test
    void 식사_후보는_샘플_5개_중_앞에서부터() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        getUrl(slot(id, "slt_2") + "/meal-candidates")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slotId").value("slt_2"))
                .andExpect(jsonPath("$.data.type").value("meal"))
                .andExpect(jsonPath("$.data.candidates", hasSize(3)))
                .andExpect(jsonPath("$.data.candidates[0].candidateId").value("cnd_m1"))
                .andExpect(jsonPath("$.data.candidates[0].kind").value("meal"))
                .andExpect(jsonPath("$.data.candidates[0].eventId").value(nullValue()))
                .andExpect(jsonPath("$.data.candidates[0].name").value("물빛 국수집"))
                .andExpect(jsonPath("$.data.candidates[0].category").value("먹거리"))
                .andExpect(jsonPath("$.data.candidates[0].placeName").value("물빛 국수집"))
                .andExpect(jsonPath("$.data.candidates[0].lat").value(nullValue()))
                .andExpect(jsonPath("$.data.candidates[0].durationMin").value(50))
                .andExpect(jsonPath("$.data.candidates[0].costAmount").value(9000))
                .andExpect(jsonPath("$.data.candidates[0].costText").value("9,000원"))
                .andExpect(jsonPath("$.data.candidates[0].distanceKmFromPrev").value(0.7))
                .andExpect(jsonPath("$.data.candidates[0].estimatedMinutes").value(nullValue()))
                .andExpect(jsonPath("$.data.candidates[0].description").value("아이와 함께 먹기 좋은 메뉴를 가정한 곳이에요."))
                .andExpect(jsonPath("$.data.candidates[0].source").value("sample"))
                .andExpect(jsonPath("$.data.candidates[2].candidateId").value("cnd_m3"));
        getUrl(slot(id, "slt_2") + "/meal-candidates?limit=5")
                .andExpect(jsonPath("$.data.candidates", hasSize(5)))
                .andExpect(jsonPath("$.data.candidates[4].name").value("숲속 도시락 카페"));
    }

    @Test
    void 식사_후보_조회_오류() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        getUrl(slot(id, "slt_2") + "/meal-candidates?limit=6").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(slot(id, "slt_3") + "/meal-candidates")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }
}
