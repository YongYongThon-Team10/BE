package com.yongyongthon.be.itinerary;

import com.jayway.jsonpath.JsonPath;
import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 9장 7, 8단계: #9 재추천, #10 공유 링크, #11 공유 미리보기 (10-1 시나리오).
 */
class ItineraryRegenerateShareApiTest extends ApiTestSupport {

    @Test
    void 일부_조건만_바꾸면_나머지는_유지하고_slotId는_새로() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(ITINERARIES + "/" + id + "/slots", "{\"type\": \"custom\", \"title\": \"카페\"}")
                .andExpect(jsonPath("$.data.slots", hasSize(6)));

        postJson(ITINERARIES + "/" + id + "/regenerate", "{\"budget\": \"free\", \"transport\": \"walk\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itineraryId").value(id))
                .andExpect(jsonPath("$.data.title").value("호수공원 가을 산책마켓 중심 하루"))
                .andExpect(jsonPath("$.data.date").value("2026-11-14"))
                .andExpect(jsonPath("$.data.transport").value("walk"))
                .andExpect(jsonPath("$.data.timeWindow.start").value("10:00"))
                .andExpect(jsonPath("$.data.slots[0].slotId").value("slt_7"))
                .andExpect(jsonPath("$.data.slots[0].moveFromPrev.mode").value("walk"))
                .andExpect(jsonPath("$.data.slots[2].eventId").value("T-NEAR3"))
                .andExpect(jsonPath("$.data.slots[?(@.title == '카페')]", hasSize(0)));
    }

    @Test
    void 재추천_오류는_기존_일정을_그대로_둔다() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        String regen = ITINERARIES + "/" + id + "/regenerate";
        postJson(regen, "{\"baseEventId\": \"T-NEAR1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("E4001"));
        postJson(regen, "{\"timeWindow\": {\"start\": \"18:00\", \"end\": \"10:00\"}}")
                .andExpect(jsonPath("$.error.code").value("E4001"));
        postJson(regen, "{\"date\": \"2026-12-25\"}")
                .andExpect(jsonPath("$.error.code").value("E4001"));
        postJson(ITINERARIES + "/itn_nope/regenerate", "{}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E4042"));
        getUrl(ITINERARIES + "/" + id)
                .andExpect(jsonPath("$.data.slots[0].slotId").value("slt_1"))
                .andExpect(jsonPath("$.data.transport").value("car"));
    }

    @Test
    void 같은_baseEventId는_허용() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(ITINERARIES + "/" + id + "/regenerate", "{\"baseEventId\": \"T-BASE\", \"date\": \"2026-11-15\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-11-15"));
    }

    @Test
    void 같은_일정은_같은_shareId() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        String first = postJson(ITINERARIES + "/" + id + "/share", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("호수공원 가을 산책마켓 중심 하루"))
                .andReturn().getResponse().getContentAsString();
        String shareId = JsonPath.read(first, "$.data.shareId");
        String shareUrl = JsonPath.read(first, "$.data.shareUrl");
        org.assertj.core.api.Assertions.assertThat(shareId).startsWith("shr_");
        org.assertj.core.api.Assertions.assertThat(shareUrl).isEqualTo("https://example.com/shared/" + shareId);

        postJson(ITINERARIES + "/" + id + "/share", "")
                .andExpect(jsonPath("$.data.shareId").value(shareId));
        postJson(ITINERARIES + "/itn_nope/share", "")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E4042"));
    }

    @Test
    void 공유_미리보기에는_slotId와_출발지가_없다() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        String share = postJson(ITINERARIES + "/" + id + "/share", "").andReturn().getResponse().getContentAsString();
        String shareId = JsonPath.read(share, "$.data.shareId");

        String json = getUrl("/api/shared/" + shareId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareId").value(shareId))
                .andExpect(jsonPath("$.data.date").value("2026-11-14"))
                .andExpect(jsonPath("$.data.companion").value("kids"))
                .andExpect(jsonPath("$.data.slotCount").value(5))
                .andExpect(jsonPath("$.data.totalCost").value(5000))
                .andExpect(jsonPath("$.data.costUnknownCount").value(2))
                .andExpect(jsonPath("$.data.slots", hasSize(5)))
                .andExpect(jsonPath("$.data.slots[0].order").value(1))
                .andExpect(jsonPath("$.data.slots[0].place.name").value("기흥 호수공원 일대"))
                .andExpect(jsonPath("$.data.slots[0].place.address").doesNotExist())
                .andExpect(jsonPath("$.data.slots[0].slotId").doesNotExist())
                .andExpect(jsonPath("$.data.slots[4].type").value("return"))
                .andExpect(jsonPath("$.data.slots[4].place").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json).doesNotContain("기흥역").doesNotContain("127.1163");

        getUrl("/api/shared/shr_nope")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E4044"));
    }
}
