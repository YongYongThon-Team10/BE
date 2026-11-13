package com.yongyongthon.be.itinerary;

import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger 문서: 오류 응답 예시와 캘린더 응답의 두 가지 모양이 문서에 들어가는지.
 */
class SwaggerDocsTest extends ApiTestSupport {

    @Test
    void 오류_응답과_캘린더_응답_모양이_문서에_있다() throws Exception {
        getUrl("/v3/api-docs")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/itineraries/{itineraryId}'].get.responses['404']"
                        + ".content['application/json'].examples.C4043").exists())
                .andExpect(jsonPath("$.paths['/api/v1/itineraries/{itineraryId}'].get.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/itineraries/{itineraryId}/slots/{slotId}'].delete.responses['400']"
                        + ".content['application/json'].examples.C4001").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/calendar'].get.responses['200']"
                        + ".content['application/json'].schema.oneOf", hasSize(2)));
    }
}
