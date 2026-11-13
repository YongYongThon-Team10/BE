package com.yongyongthon.be.itinerary;

import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 9장 5단계: #3 추가, #4 삭제, #5 교체, #8 순서 변경 (10-1 시나리오).
 * 기본 일정(T-BASE, 2026-11-14): slt_1 event, slt_2 meal, slt_3 T-NEAR1, slt_4 T-NEAR2, slt_5 return
 */
class ItinerarySlotApiTest extends ApiTestSupport {

    private String slots(String id) {
        return ITINERARIES + "/" + id + "/slots";
    }

    @Test
    void 빈_식사_슬롯이_있으면_그_슬롯을_채운다() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(slots(id), """
                {"type": "meal", "title": "호수 옆 국밥", "durationMin": 40}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots", hasSize(5)))
                .andExpect(jsonPath("$.data.slots[1].slotId").value("slt_2"))
                .andExpect(jsonPath("$.data.slots[1].title").value("호수 옆 국밥"))
                .andExpect(jsonPath("$.data.slots[1].durationMin").value(40))
                .andExpect(jsonPath("$.data.slots[1].costAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[1].isUserAdded").value(true));
    }

    @Test
    void 빈_식사_슬롯이_없으면_새_식사_슬롯을_귀가_직전에_추가() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(slots(id), "{\"type\": \"meal\", \"title\": \"점심\"}");
        postJson(slots(id), "{\"type\": \"meal\", \"title\": \"간식\", \"costText\": \"5,000원\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots", hasSize(6)))
                .andExpect(jsonPath("$.data.slots[4].title").value("간식"))
                .andExpect(jsonPath("$.data.slots[4].slotId").value("slt_6"))
                .andExpect(jsonPath("$.data.slots[4].durationMin").value(45))
                .andExpect(jsonPath("$.data.slots[4].costAmount").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[5].type").value("return"));
    }

    @Test
    void 근처_행사를_지정한_슬롯_뒤에_추가() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(slots(id), """
                {"type": "nearbyEvent", "eventId": "T-NEAR3", "afterSlotId": "slt_1"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots", hasSize(6)))
                .andExpect(jsonPath("$.data.slots[1].eventId").value("T-NEAR3"))
                .andExpect(jsonPath("$.data.slots[1].order").value(2))
                .andExpect(jsonPath("$.data.slots[1].durationMin").value(90))
                .andExpect(jsonPath("$.data.slots[1].isUserAdded").value(true))
                .andExpect(jsonPath("$.data.slots[2].type").value("meal"))
                .andExpect(jsonPath("$.data.slots[2].order").value(3))
                .andExpect(jsonPath("$.data.slots[5].type").value("return"));
    }

    @Test
    void 귀가_뒤를_지정해도_귀가_직전에_추가() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(slots(id), """
                {"type": "custom", "title": "카페", "afterSlotId": "slt_5"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[4].title").value("카페"))
                .andExpect(jsonPath("$.data.slots[5].type").value("return"));
    }

    @Test
    void 직접_입력_비용과_주소() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(slots(id), """
                {"type": "custom", "title": "공방 체험", "costText": "12,000원", "address": "용인시 어딘가"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[4].costAmount").value(12000))
                .andExpect(jsonPath("$.data.slots[4].costText").value("12,000원"))
                .andExpect(jsonPath("$.data.slots[4].durationMin").value(30))
                .andExpect(jsonPath("$.data.slots[4].place.address").value("용인시 어딘가"))
                .andExpect(jsonPath("$.data.slots[4].place.lat").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[4].moveFromPrev").value(nullValue()))
                .andExpect(jsonPath("$.data.totalCost").value(17000))
                .andExpect(jsonPath("$.data.warnings[?(@.code == 'COORD_MISSING')].slotId").value("slt_6"));
    }

    @Test
    void 머무는_시간_범위_오류는_C001() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(slots(id), "{\"type\": \"custom\", \"title\": \"a\", \"durationMin\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(slots(id), "{\"type\": \"custom\", \"title\": \"a\", \"durationMin\": 301}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 추가_요청_오류() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(slots(id), "{\"title\": \"a\"}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(slots(id), "{\"type\": \"return\"}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(slots(id), "{\"type\": \"nearbyEvent\"}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(slots(id), "{\"type\": \"custom\", \"title\": \"" + "가".repeat(51) + "\"}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(slots(id), "{\"type\": \"program\", \"eventId\": \"T-BASE\", \"programId\": \"p1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(slots(id), "{\"type\": \"nearbyEvent\", \"eventId\": \"NO-SUCH\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4042"));
        postJson(slots(id), "{\"type\": \"custom\", \"title\": \"a\", \"afterSlotId\": \"slt_99\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4044"));
        postJson(slots("itn_nope"), "{\"type\": \"custom\", \"title\": \"a\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4043"));
    }

    @Test
    void 기준_행사와_귀가_슬롯은_삭제_불가_C4001() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        deleteUrl(slots(id) + "/slt_1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C4001"))
                .andExpect(jsonPath("$.error.message").value("삭제할 수 없는 항목입니다."));
        deleteUrl(slots(id) + "/slt_5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C4001"));
    }

    @Test
    void 슬롯_삭제_후_재계산() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        deleteUrl(slots(id) + "/slt_3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots", hasSize(4)))
                .andExpect(jsonPath("$.data.slots[2].slotId").value("slt_4"))
                .andExpect(jsonPath("$.data.slots[2].order").value(3))
                .andExpect(jsonPath("$.data.totalCost").value(0))
                .andExpect(jsonPath("$.data.warnings", hasSize(0)));
        deleteUrl(slots(id) + "/slt_99")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4044"));
    }

    @Test
    void 교체_요청은_둘_중_하나만() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson(slots(id) + "/slt_3", "{\"replaceWithCandidateId\": \"cnd_1\", \"replaceWithEventId\": \"T-NEAR3\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        putJson(slots(id) + "/slt_3", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 귀가와_기준_행사_슬롯은_교체_불가() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson(slots(id) + "/slt_5", "{\"replaceWithEventId\": \"T-NEAR3\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        putJson(slots(id) + "/slt_1", "{\"replaceWithEventId\": \"T-NEAR3\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 식사_슬롯을_식사_후보로_교체() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson(slots(id) + "/slt_2", "{\"replaceWithCandidateId\": \"cnd_m1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[1].slotId").value("slt_2"))
                .andExpect(jsonPath("$.data.slots[1].title").value("물빛 국수집"))
                .andExpect(jsonPath("$.data.slots[1].durationMin").value(50))
                .andExpect(jsonPath("$.data.slots[1].costAmount").value(9000))
                .andExpect(jsonPath("$.data.slots[1].costText").value("9,000원"))
                .andExpect(jsonPath("$.data.slots[1].place").value(nullValue()))
                .andExpect(jsonPath("$.data.slots[1].isUserAdded").value(false))
                .andExpect(jsonPath("$.data.totalCost").value(14000))
                .andExpect(jsonPath("$.data.costUnknownCount").value(1));
    }

    @Test
    void 슬롯_종류와_후보_종류가_다르면_C001() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson(slots(id) + "/slt_3", "{\"replaceWithCandidateId\": \"cnd_m1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        putJson(slots(id) + "/slt_2", "{\"replaceWithCandidateId\": \"cnd_1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        putJson(slots(id) + "/slt_2", "{\"replaceWithEventId\": \"T-NEAR3\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 모르는_후보는_404_C4044() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson(slots(id) + "/slt_3", "{\"replaceWithCandidateId\": \"cnd_99\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4044"));
        putJson(slots(id) + "/slt_2", "{\"replaceWithCandidateId\": \"cnd_m9\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4044"));
    }

    @Test
    void 행사로_직접_교체() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson(slots(id) + "/slt_3", "{\"replaceWithEventId\": \"T-NEAR3\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[2].slotId").value("slt_3"))
                .andExpect(jsonPath("$.data.slots[2].type").value("nearbyEvent"))
                .andExpect(jsonPath("$.data.slots[2].eventId").value("T-NEAR3"))
                .andExpect(jsonPath("$.data.slots[2].costText").value("무료"))
                .andExpect(jsonPath("$.data.totalCost").value(0));
        putJson(slots(id) + "/slt_3", "{\"replaceWithEventId\": \"NO-SUCH\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4042"));
    }

    @Test
    void 순서_변경_오류() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        String url = slots(id) + "/order";
        patchJson(url, "{\"slotIds\": [\"slt_1\", \"slt_2\", \"slt_3\", \"slt_4\", \"slt_5\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        patchJson(url, "{\"slotIds\": [\"slt_1\", \"slt_2\", \"slt_3\"]}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        patchJson(url, "{\"slotIds\": [\"slt_1\", \"slt_2\", \"slt_3\", \"slt_3\"]}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        patchJson(url, "{\"slotIds\": [\"slt_1\", \"slt_2\", \"slt_3\", \"slt_9\"]}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        patchJson(url, "{}")
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 순서_변경_후_시간_재계산과_귀가_마지막() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        patchJson(slots(id) + "/order", "{\"slotIds\": [\"slt_3\", \"slt_1\", \"slt_2\", \"slt_4\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[0].slotId").value("slt_3"))
                .andExpect(jsonPath("$.data.slots[0].order").value(1))
                .andExpect(jsonPath("$.data.slots[0].startTime").value("10:05"))
                .andExpect(jsonPath("$.data.slots[1].slotId").value("slt_1"))
                .andExpect(jsonPath("$.data.slots[1].startTime").value("11:10"))
                .andExpect(jsonPath("$.data.slots[4].slotId").value("slt_5"))
                .andExpect(jsonPath("$.data.slots[4].type").value("return"))
                .andExpect(jsonPath("$.data.slots[4].order").value(5));
    }
}
