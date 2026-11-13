package com.yongyongthon.be.course;

import com.jayway.jsonpath.JsonPath;
import com.yongyongthon.be.course.data.repository.CourseRepository;
import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 9장 9, 10단계: #12 저장, #13 목록, #14 삭제, #15 캘린더 (10-1 시나리오).
 * 사용자 구분이 없어서 테스트마다 코스를 비우고 시작한다.
 */
class CourseCalendarApiTest extends ApiTestSupport {

    private static final String COURSES = "/api/v1/me/courses";
    private static final String CALENDAR = "/api/v1/me/calendar";

    @Autowired
    CourseRepository courseRepository;

    @BeforeEach
    void clearCourses() {
        courseRepository.deleteAll();
    }

    @Test
    void 저장한_코스가_없으면_빈_목록() throws Exception {
        getUrl(COURSES)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.courses", hasSize(0)));
    }

    @Test
    void 같은_일정을_두_번_저장하면_같은_courseId() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        String first = postJson(COURSES, "{\"itineraryId\": \"" + id + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itineraryId").value(id))
                .andExpect(jsonPath("$.data.title").value("호수공원 가을 산책마켓 중심 하루"))
                .andExpect(jsonPath("$.data.isSaved").value(true))
                .andReturn().getResponse().getContentAsString();
        String courseId = JsonPath.read(first, "$.data.courseId");
        String savedAt = JsonPath.read(first, "$.data.savedAt");
        org.assertj.core.api.Assertions.assertThat(courseId).startsWith("crs_");
        org.assertj.core.api.Assertions.assertThat(savedAt).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");

        postJson(COURSES, "{\"itineraryId\": \"" + id + "\", \"title\": \"아이랑 호수공원\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("아이랑 호수공원"));

        getUrl("/api/v1/itineraries/" + id).andExpect(jsonPath("$.data.isSaved").value(true));
        getUrl(COURSES).andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    void 저장_요청_오류() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(COURSES, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(COURSES, "{\"itineraryId\": \"" + id + "\", \"title\": \"" + "가".repeat(41) + "\"}")
                .andExpect(jsonPath("$.error.code").value("C001"));
        postJson(COURSES, "{\"itineraryId\": \"itn_nope\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4043"));
    }

    @Test
    void 코스_목록_필드() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        putJson("/api/v1/itineraries/" + id + "/slots/slt_2", "{\"replaceWithCandidateId\": \"cnd_m3\"}");
        postJson(COURSES, "{\"itineraryId\": \"" + id + "\"}");

        getUrl(COURSES)
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].itineraryId").value(id))
                .andExpect(jsonPath("$.data.courses[0].date").value("2026-11-14"))
                .andExpect(jsonPath("$.data.courses[0].companion").value("kids"))
                .andExpect(jsonPath("$.data.courses[0].transport").value("car"))
                .andExpect(jsonPath("$.data.courses[0].slotCount").value(5))
                .andExpect(jsonPath("$.data.courses[0].summary")
                        .value("호수공원 가을 산책마켓 \u00B7 주변 먹거리 \u00B7 볼거리 \u00B7 귀가까지 5개 일정"))
                .andExpect(jsonPath("$.data.courses[0].mealSummary").value("호수 옆 작은 식탁"))
                .andExpect(jsonPath("$.data.courses[0].coverImageUrl").value(nullValue()));
    }

    @Test
    void 빈_식사_슬롯이면_mealSummary_null() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(COURSES, "{\"itineraryId\": \"" + id + "\"}");
        getUrl(COURSES).andExpect(jsonPath("$.data.courses[0].mealSummary").value(nullValue()));
    }

    @Test
    void 목록은_최근_저장_순과_페이지() throws Exception {
        String a = createItinerary("T-BASE", "2026-11-14");
        String b = createItinerary("T-BASE", "2026-11-15");
        postJson(COURSES, "{\"itineraryId\": \"" + a + "\"}");
        postJson(COURSES, "{\"itineraryId\": \"" + b + "\"}");
        getUrl(COURSES).andExpect(jsonPath("$.data.courses[0].itineraryId").value(b));
        getUrl(COURSES + "?page=1&size=1")
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.courses", hasSize(1)))
                .andExpect(jsonPath("$.data.courses[0].itineraryId").value(a));
        getUrl(COURSES + "?page=-1").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(COURSES + "?size=0").andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    void 코스를_지우면_목록에서_빠지고_일정은_남는다() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        String saved = postJson(COURSES, "{\"itineraryId\": \"" + id + "\"}").andReturn().getResponse().getContentAsString();
        String courseId = JsonPath.read(saved, "$.data.courseId");

        deleteUrl(COURSES + "/" + courseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.deleted").value(true));
        getUrl(COURSES).andExpect(jsonPath("$.data.totalCount").value(0));
        getUrl("/api/v1/itineraries/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isSaved").value(false));

        deleteUrl(COURSES + "/" + courseId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4043"));
        deleteUrl(COURSES + "/nope")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("C4043"));

        String again = postJson(COURSES, "{\"itineraryId\": \"" + id + "\"}").andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(again, "$.data.courseId")).isNotEqualTo(courseId);
    }

    @Test
    void 캘린더_월과_날짜() throws Exception {
        String id = createItinerary("T-BASE", "2026-11-14");
        postJson(COURSES, "{\"itineraryId\": \"" + id + "\"}");

        getUrl(CALENDAR + "?month=2026-11")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value("2026-11"))
                .andExpect(jsonPath("$.data.events", hasSize(0)))
                .andExpect(jsonPath("$.data.courses", hasSize(1)))
                .andExpect(jsonPath("$.data.courses[0].itineraryId").value(id))
                .andExpect(jsonPath("$.data.courses[0].title").value("호수공원 가을 산책마켓 중심 하루"))
                .andExpect(jsonPath("$.data.courses[0].date").value("2026-11-14"));
        getUrl(CALENDAR + "?month=2026-12").andExpect(jsonPath("$.data.courses", hasSize(0)));

        getUrl(CALENDAR + "?date=2026-11-14")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-11-14"))
                .andExpect(jsonPath("$.data.eventCount").value(0))
                .andExpect(jsonPath("$.data.courseCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].slotCount").value(5));
        getUrl(CALENDAR + "?date=2026-11-15").andExpect(jsonPath("$.data.courseCount").value(0));
    }

    @Test
    void 캘린더는_month와_date_중_하나만() throws Exception {
        getUrl(CALENDAR + "?month=2026-11&date=2026-11-14")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(CALENDAR).andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(CALENDAR + "?month=2026-13").andExpect(jsonPath("$.error.code").value("C001"));
        getUrl(CALENDAR + "?date=2026/11/14").andExpect(jsonPath("$.error.code").value("C001"));
    }
}
