package com.yongyongthon.be.event;

import com.yongyongthon.be.common.response.GlobalExceptionHandler;
import com.yongyongthon.be.common.response.PublicApiExceptionHandler;
import com.yongyongthon.be.event.controller.EventController;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.event.data.repository.EventSourceRepository;
import com.yongyongthon.be.event.data.repository.FavoriteRepository;
import com.yongyongthon.be.event.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DB 없이 E1의 필터, 정렬, 페이지, 지도 보기, 오류 코드를 확인한다. 조회 기준일은 2026-10-17.
 */
class EventListApiTests {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private MockMvc mockMvc;

    private static Event event(String id, String start, String end, String status, String district,
                               String address, Boolean isFree, Double lat, Double lng) {
        return Event.builder()
                .id(id).name(id).category("행사").dateStatus(status)
                .startDate(LocalDate.parse(start)).endDate(LocalDate.parse(end))
                .district(district).address(address).isFree(isFree).lat(lat).lng(lng)
                .sourceUrl("https://example")
                .build();
    }

    @BeforeEach
    void setUp() {
        when(eventRepository.findAll()).thenReturn(List.of(
                // 진행 중: 기흥구, 무료, 기준점에서 먼 곳
                event("E-ONGOING", "2026-10-01", "2026-10-31", "확정", "기흥구",
                        "경기도 용인시 기흥구 신갈동 1", true, 37.30, 127.20),
                // 시작 예정: 수지구, 좌표 없음
                event("E-UPCOMING", "2026-10-17", "2026-10-18", "예정", "수지구", null, null, null, null),
                // 연간 계획: 처인구, 기준점과 같은 곳
                event("E-ANNUAL", "2026-01-01", "2026-12-31", "연간 계획", "처인구",
                        "경기도 용인시 처인구 마평동 2", false, 37.23, 127.20),
                // 기간 밖
                event("E-PAST", "2026-09-01", "2026-09-30", "확정", "수지구", null, true, 37.3, 127.1)
        ));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(new EventService(eventRepository, mock(EventSourceRepository.class), mock(FavoriteRepository.class))))
                .setControllerAdvice(new PublicApiExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 날짜순은_진행중_시작예정_연간계획_순서이고_기간밖은_빠진다() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.events[*].eventId").value(contains("E-ONGOING", "E-UPCOMING", "E-ANNUAL")))
                .andExpect(jsonPath("$.data.events[0].tags[0]").value("무료"))
                .andExpect(jsonPath("$.data.events[0].distanceKm").value(nullValue()))
                .andExpect(jsonPath("$.data.events[0].isLiked").value(false))
                .andExpect(jsonPath("$.data.events[0].hasDiscrepancy").value(false));
    }

    @Test
    void 구는_콤마로_여러개_받는다() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17").param("district", "수지구,처인구"))
                .andExpect(jsonPath("$.data.events[*].eventId").value(contains("E-UPCOMING", "E-ANNUAL")));
    }

    @Test
    void 동은_주소에_동이름이_있는_행사만이고_주소없는_행사는_빠진다() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17").param("dong", "신갈동"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.events[0].eventId").value("E-ONGOING"));
    }

    @Test
    void 무료는_무료로_확인된_행사만() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17").param("budget", "free"))
                .andExpect(jsonPath("$.data.events[*].eventId").value(contains("E-ONGOING")));
    }

    @Test
    void 거리순은_가까운순이고_좌표없는_행사는_맨뒤() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17")
                        .param("sort", "distance").param("lat", "37.23").param("lng", "127.20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events[*].eventId").value(contains("E-ANNUAL", "E-ONGOING", "E-UPCOMING")))
                .andExpect(jsonPath("$.data.events[0].distanceKm").value(0.0))
                .andExpect(jsonPath("$.data.events[1].distanceKm").value(7.78))
                .andExpect(jsonPath("$.data.events[2].distanceKm").value(nullValue()));
    }

    @Test
    void 지도보기는_페이지없이_전체를_주고_목록과_건수가_같다() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17").param("view", "map")
                        .param("page", "1").param("size", "1"))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.events.length()").value(3));
    }

    @Test
    void 페이지는_0부터() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17").param("page", "1").param("size", "1"))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.events[*].eventId").value(contains("E-UPCOMING")));
    }

    @Test
    void 결과가_없으면_200과_빈배열() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2027-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.events").isEmpty());
    }

    @Test
    void 거리순에_좌표가_없으면_E4002() throws Exception {
        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17").param("sort", "distance"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("E4002"));
    }

    @Test
    void 요청값_오류는_E4001() throws Exception {
        expectE4001(get("/api/events"));
        expectE4001(get("/api/events").param("dateFrom", "2026-13-01"));
        expectE4001(get("/api/events").param("dateFrom", "2026-10-18").param("dateTo", "2026-10-17"));
        expectE4001(get("/api/events").param("dateFrom", "2026-10-17").param("district", "강남구"));
        expectE4001(get("/api/events").param("dateFrom", "2026-10-17").param("sort", "name"));
        expectE4001(get("/api/events").param("dateFrom", "2026-10-17").param("view", "grid"));
        expectE4001(get("/api/events").param("dateFrom", "2026-10-17").param("size", "0"));
    }

    private void expectE4001(RequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("E4001"));
    }
}
