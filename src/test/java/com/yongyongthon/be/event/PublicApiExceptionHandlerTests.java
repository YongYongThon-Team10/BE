package com.yongyongthon.be.event;

import com.yongyongthon.be.common.response.GlobalExceptionHandler;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.response.PublicApiExceptionHandler;
import com.yongyongthon.be.user.controller.AuthController;
import com.yongyongthon.be.user.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DB 없이 돌아가는 테스트. event 패키지 컨트롤러는 E4001, 인증 API는 기존 C002를 받는지 확인한다.
 */
class PublicApiExceptionHandlerTests {

    @RestController
    static class SampleEventController {
        @GetMapping("/api/events")
        GlobalResponse<String> list(@RequestParam LocalDate dateFrom,
                                    @RequestParam(required = false) @Size(max = 3) String dong) {
            return GlobalResponse.ok("ok");
        }

        @PostMapping("/api/events")
        GlobalResponse<String> create(@Valid @RequestBody Body body) {
            return GlobalResponse.ok("ok");
        }

        record Body(@NotBlank String query) {
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SampleEventController(), new AuthController(Mockito.mock(AuthService.class)))
                .setControllerAdvice(new PublicApiExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 정상_요청은_통과한다() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void 필수_파라미터_누락은_E4001() throws Exception {
        expectE4001(get("/api/events"));
    }

    @Test
    void 날짜_형식_오류는_E4001() throws Exception {
        expectE4001(get("/api/events").param("dateFrom", "2026/10/17"));
    }

    @Test
    void 파라미터_제약_위반은_E4001() throws Exception {
        expectE4001(get("/api/events").param("dateFrom", "2026-10-17").param("dong", "아주긴동이름"));
    }

    @Test
    void 본문_검증_실패는_E4001() throws Exception {
        expectE4001(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"\"}"));
    }

    @Test
    void 깨진_JSON은_E4001() throws Exception {
        expectE4001(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{"));
    }

    @Test
    void 인증_API_검증_실패는_기존대로_C002() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signin").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("C002"));
    }

    private void expectE4001(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("E4001"));
    }
}
