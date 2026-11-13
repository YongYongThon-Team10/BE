package com.yongyongthon.be.itinerary;

import com.yongyongthon.be.common.response.GlobalExceptionHandler;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.response.PublicApiExceptionHandler;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.event.data.repository.EventSourceRepository;
import com.yongyongthon.be.event.data.repository.FavoriteRepository;
import com.yongyongthon.be.event.service.EventService;
import com.yongyongthon.be.itinerary.data.dto.ItineraryCreateRequestDto;
import com.yongyongthon.be.itinerary.service.ItineraryRequestValidator;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DB 없이 E5 요청 검증을 확인한다. 실제 컨트롤러는 S5 담당이므로, 같은 패키지의 시험용 컨트롤러로
 * "@Valid @RequestBody + validator 호출"이라는 사용법 그대로 검사한다.
 */
class ItineraryRequestValidationTests {

    @RestController
    static class SampleItineraryController {
        private final ItineraryRequestValidator validator;

        SampleItineraryController(ItineraryRequestValidator validator) {
            this.validator = validator;
        }

        @PostMapping("/api/itineraries")
        GlobalResponse<String> create(@Valid @RequestBody ItineraryCreateRequestDto request) {
            return GlobalResponse.ok(validator.validate(request).getId());
        }
    }

    private static final String VALID = """
            {"baseEventId":"YI-0001","date":"2026-10-17","companion":"kids","kidsAge":5,
             "origin":{"type":"manual","lat":37.27,"lng":127.11,"address":"기흥역"},
             "transport":"transit","timeWindow":{"start":"10:00","end":"18:00"},
             "budget":"free","wants":["food","rest"]}
            """;

    private final EventRepository eventRepository = mock(EventRepository.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EventService eventService = new EventService(eventRepository, mock(EventSourceRepository.class),
                mock(FavoriteRepository.class));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SampleItineraryController(new ItineraryRequestValidator(eventService)))
                .setControllerAdvice(new PublicApiExceptionHandler(), new GlobalExceptionHandler())
                .build();
        when(eventRepository.findById("YI-0001")).thenReturn(Optional.of(Event.builder().id("YI-0001").build()));
        when(eventRepository.findById("YI-9999")).thenReturn(Optional.empty());
    }

    @Test
    void 정상_요청은_기준_행사를_돌려준다() throws Exception {
        perform(VALID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("YI-0001"));
    }

    @Test
    void 선택_항목은_생략해도_된다() throws Exception {
        perform("""
                {"baseEventId":"YI-0001","date":"2026-10-17",
                 "origin":{"type":"current","lat":37.27,"lng":127.11},
                 "transport":"car","timeWindow":{"start":"10:00","end":"18:00"}}
                """).andExpect(status().isOk());
    }

    @Test
    void 없는_기준_행사는_E4041() throws Exception {
        perform(VALID.replace("YI-0001", "YI-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E4041"));
    }

    @Test
    void 요청값_오류는_E4001() throws Exception {
        // 시간대 역전과 같은 시각
        expectE4001(VALID.replace("\"end\":\"18:00\"", "\"end\":\"09:00\""));
        expectE4001(VALID.replace("\"end\":\"18:00\"", "\"end\":\"10:00\""));
        // 출발지 좌표 없음 (직접 입력 후 검색 결과를 고르지 않음)
        expectE4001(VALID.replace("\"lat\":37.27,", ""));
        // 출발지, 시간대 누락
        expectE4001(VALID.replace("\"origin\":{\"type\":\"manual\",\"lat\":37.27,\"lng\":127.11,\"address\":\"기흥역\"},", ""));
        expectE4001(VALID.replace("\"timeWindow\":{\"start\":\"10:00\",\"end\":\"18:00\"},", ""));
        // 허용되지 않는 값
        expectE4001(VALID.replace("\"transit\"", "\"bike\""));
        expectE4001(VALID.replace("\"rest\"", "\"shopping\""));
        expectE4001(VALID.replace("\"kids\"", "\"pets\""));
        expectE4001(VALID.replace("\"kidsAge\":5", "\"kidsAge\":-1"));
        // 형식 오류
        expectE4001(VALID.replace("\"10:00\"", "\"25:00\""));
        expectE4001(VALID.replace("\"2026-10-17\"", "\"2026/10/17\""));
        expectE4001(VALID.replace("\"baseEventId\":\"YI-0001\",", ""));
    }

    private org.springframework.test.web.servlet.ResultActions perform(String body) throws Exception {
        return mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void expectE4001(String body) throws Exception {
        perform(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("E4001"));
    }
}
