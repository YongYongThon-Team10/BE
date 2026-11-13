package com.yongyongthon.be.event;

import com.yongyongthon.be.common.response.GlobalExceptionHandler;
import com.yongyongthon.be.common.response.PublicApiExceptionHandler;
import com.yongyongthon.be.event.controller.EventController;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.data.entity.EventSource;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.event.data.repository.EventSourceRepository;
import com.yongyongthon.be.event.data.repository.FavoriteRepository;
import com.yongyongthon.be.event.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DB 없이 E2 응답 형태(키 이름, null 유지, 출처 순서, 404)를 확인한다.
 */
class EventDetailApiTests {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventSourceRepository eventSourceRepository = mock(EventSourceRepository.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(new EventService(eventRepository, eventSourceRepository, mock(FavoriteRepository.class))))
                .setControllerAdvice(new PublicApiExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 상세_응답은_명세_키와_null을_유지한다() throws Exception {
        Event event = Event.builder()
                .id("YI-0001").name("도서관 그림책 교실").category("행사").subCategory("도서관 프로그램")
                .startDate(LocalDate.of(2026, 10, 17)).endDate(LocalDate.of(2026, 10, 17)).dateStatus("확정")
                .placeName("수지도서관").district("수지구").lat(37.3).lng(127.1).coordPrecision("place")
                .isFree(true).applyType("apply").applyUrl("https://lib.yongin.go.kr/intro")
                .receiptStart(LocalDateTime.of(2025, 12, 3, 10, 0))
                .sourceUrl("https://blog.example")
                .build();
        EventSource blog = EventSource.builder().event(event).name("용인시 공식 블로그").type("blog")
                .url("https://blog.example").sortOrder(0).build();
        EventSource official = EventSource.builder().event(event).name("용인시청 보도자료").type("official")
                .url("https://www.yongin.go.kr").sortOrder(1).build();
        when(eventRepository.findById("YI-0001")).thenReturn(Optional.of(event));
        when(eventSourceRepository.findByEventIdOrderBySortOrder("YI-0001")).thenReturn(List.of(blog, official));

        mockMvc.perform(get("/api/events/YI-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value("YI-0001"))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.hasDiscrepancy").value(false))
                .andExpect(jsonPath("$.data.tags[0]").value("무료"))
                .andExpect(jsonPath("$.data.tags[1]").value("신청 필요"))
                .andExpect(jsonPath("$.data", hasKey("summary")))
                .andExpect(jsonPath("$.data.summary").value(nullValue()))
                .andExpect(jsonPath("$.data.keyInfo.startDate").value("2026-10-17"))
                .andExpect(jsonPath("$.data.keyInfo.isFree").value(true))
                .andExpect(jsonPath("$.data.keyInfo", hasKey("time")))
                .andExpect(jsonPath("$.data.apply.type").value("apply"))
                .andExpect(jsonPath("$.data.apply.text").value("신청 필요"))
                .andExpect(jsonPath("$.data.apply.receiptStart").value("2025-12-03 10:00"))
                .andExpect(jsonPath("$.data.programs").isEmpty())
                .andExpect(jsonPath("$.data.sources[0].type").value("official"))
                .andExpect(jsonPath("$.data.sources[1].type").value("blog"))
                .andExpect(jsonPath("$.error.code").value(nullValue()));
    }

    @Test
    void 안내_상이는_데이터가_생기기_전까지_항상_없음() throws Exception {
        when(eventRepository.findById("YI-0001")).thenReturn(Optional.of(
                Event.builder().id("YI-0001").name("행사").category("행사").dateStatus("확정").sourceUrl("https://a").build()));

        mockMvc.perform(get("/api/events/YI-0001/discrepancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value("YI-0001"))
                .andExpect(jsonPath("$.data.hasDiscrepancy").value(false))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 없는_행사의_안내_상이는_E4041() throws Exception {
        when(eventRepository.findById("YI-9999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/YI-9999/discrepancies"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E4041"));
    }

    @Test
    void 없는_행사는_E4041() throws Exception {
        when(eventRepository.findById("YI-9999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/YI-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("E4041"));
    }
}
