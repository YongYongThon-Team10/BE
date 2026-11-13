package com.yongyongthon.be.course;

import com.jayway.jsonpath.JsonPath;
import com.yongyongthon.be.course.data.repository.CourseRepository;
import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 지시서 10-2 스모크 테스트: 1번부터 10번까지 한 흐름으로 호출한다.
 */
class SmokeFlowTest extends ApiTestSupport {

    @Autowired
    CourseRepository courseRepository;

    @Test
    void 처음부터_끝까지() throws Exception {
        courseRepository.deleteAll();

        // 1. 일정 생성
        String created = postJson(ITINERARIES, createBody("T-BASE", "2026-11-14", "10:00", "18:00"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.data.itineraryId");
        String base = ITINERARIES + "/" + id;

        // 2. 같은 일정 조회
        getUrl(base).andExpect(status().isOk()).andExpect(jsonPath("$.data.itineraryId").value(id));

        // 3. 빈 식사 슬롯 채우기
        postJson(base + "/slots", "{\"type\": \"meal\", \"title\": \"점심\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[1].slotId").value("slt_2"))
                .andExpect(jsonPath("$.data.slots[1].title").value("점심"));

        // 4. 식사 후보 조회 후 교체
        getUrl(base + "/slots/slt_2/meal-candidates").andExpect(status().isOk());
        putJson(base + "/slots/slt_2", "{\"replaceWithCandidateId\": \"cnd_m2\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[1].title").value("산책길 브런치"));

        // 5. 순서 변경
        patchJson(base + "/slots/order", "{\"slotIds\": [\"slt_1\", \"slt_3\", \"slt_2\", \"slt_4\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slots[1].slotId").value("slt_3"))
                .andExpect(jsonPath("$.data.slots[4].type").value("return"));

        // 6. 코스 저장
        String saved = postJson("/api/v1/me/courses", "{\"itineraryId\": \"" + id + "\"}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String courseId = JsonPath.read(saved, "$.data.courseId");

        // 7. 목록에 있음
        getUrl("/api/v1/me/courses")
                .andExpect(jsonPath("$.data.courses[0].courseId").value(courseId))
                .andExpect(jsonPath("$.data.courses[0].mealSummary").value("산책길 브런치"));

        // 8. 캘린더에 있음
        getUrl("/api/v1/me/calendar?month=2026-11")
                .andExpect(jsonPath("$.data.courses[0].itineraryId").value(id));

        // 9. 공유 링크와 미리보기
        String share = postJson(base + "/share", "").andReturn().getResponse().getContentAsString();
        String shareId = JsonPath.read(share, "$.data.shareId");
        getUrl("/api/v1/shared/" + shareId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slotCount").value(5));

        // 10. 코스 삭제 후 목록이 비어 있음
        deleteUrl("/api/v1/me/courses/" + courseId).andExpect(status().isOk());
        getUrl("/api/v1/me/courses").andExpect(jsonPath("$.data.courses", hasSize(0)));
    }
}
