package com.yongyongthon.be.event;

import com.yongyongthon.be.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger 문서에 신형 API(E1, E2, E3, E4, E14, E19, E20)의 오류 응답 예시가 들어가는지.
 */
class EventSwaggerDocsTest extends ApiTestSupport {

    private static final String EXAMPLES = ".content['application/json'].examples.";

    @Test
    void 신형_API의_오류_응답_예시가_문서에_있다() throws Exception {
        getUrl("/v3/api-docs")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/events'].get.responses['400']" + EXAMPLES + "E4001").exists())
                .andExpect(jsonPath("$.paths['/api/events'].get.responses['400']" + EXAMPLES + "E4002").exists())
                .andExpect(jsonPath("$.paths['/api/events/{eventId}'].get.responses['404']" + EXAMPLES + "E4041").exists())
                .andExpect(jsonPath("$.paths['/api/events/{eventId}/discrepancies'].get.responses['404']" + EXAMPLES + "E4041").exists())
                .andExpect(jsonPath("$.paths['/api/me/favorites/{eventId}'].put.responses['401']" + EXAMPLES + "C4011").exists())
                .andExpect(jsonPath("$.paths['/api/me/favorites/{eventId}'].delete.responses['404']" + EXAMPLES + "E4041").exists())
                .andExpect(jsonPath("$.paths['/api/search/parse'].post.responses['502']" + EXAMPLES + "E5021").exists())
                .andExpect(jsonPath("$.paths['/api/search/parse'].post.responses['504']" + EXAMPLES + "E5041").exists())
                .andExpect(jsonPath("$.paths['/api/places/search'].get.responses['502']" + EXAMPLES + "E5022").exists())
                .andExpect(jsonPath("$.paths['/api/places/search'].get.responses['500']").exists());
    }
}
