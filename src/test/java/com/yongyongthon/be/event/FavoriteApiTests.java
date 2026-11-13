package com.yongyongthon.be.event;

import com.yongyongthon.be.common.response.GlobalExceptionHandler;
import com.yongyongthon.be.common.response.PublicApiExceptionHandler;
import com.yongyongthon.be.common.security.SecurityUtil;
import com.yongyongthon.be.event.controller.EventController;
import com.yongyongthon.be.event.controller.FavoriteController;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.data.entity.Favorite;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.event.data.repository.EventSourceRepository;
import com.yongyongthon.be.event.data.repository.FavoriteRepository;
import com.yongyongthon.be.event.service.EventService;
import com.yongyongthon.be.event.service.FavoriteService;
import com.yongyongthon.be.user.data.entity.User;
import com.yongyongthon.be.user.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DB 없이 관심 행사 담기와 해제(E19, E20), 그리고 E1, E2의 isLiked 반영을 확인한다.
 * 토큰이 없을 때의 401은 보안 필터가 처리하므로 이 테스트 범위 밖이다.
 */
class FavoriteApiTests {

    private static final String EMAIL = "user@example.com";

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final AuthService authService = mock(AuthService.class);
    private MockMvc mockMvc;

    private final Event event = Event.builder()
            .id("YI-0001").name("행사").category("행사").dateStatus("확정")
            .startDate(LocalDate.of(2026, 10, 17)).endDate(LocalDate.of(2026, 10, 17))
            .sourceUrl("https://a").build();

    @BeforeEach
    void setUp() {
        EventService eventService = new EventService(eventRepository, mock(EventSourceRepository.class), favoriteRepository);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(eventService),
                        new FavoriteController(new FavoriteService(favoriteRepository, eventService, authService)))
                .setControllerAdvice(new PublicApiExceptionHandler(), new GlobalExceptionHandler())
                .build();
        when(eventRepository.findById("YI-0001")).thenReturn(Optional.of(event));
        when(eventRepository.findById("YI-9999")).thenReturn(Optional.empty());
        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(authService.getUserEntity(EMAIL)).thenReturn(User.builder().id(1L).email(EMAIL).build());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, "", AuthorityUtils.createAuthorityList("ROLE_USER")));
    }

    @Test
    void 담기는_처음이면_저장하고_favorited_true() throws Exception {
        login();
        when(favoriteRepository.existsByUserEmailAndEventId(EMAIL, "YI-0001")).thenReturn(false);

        mockMvc.perform(put("/api/me/favorites/YI-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value("YI-0001"))
                .andExpect(jsonPath("$.data.favorited").value(true));
        verify(favoriteRepository).saveAndFlush(any(Favorite.class));
    }

    @Test
    void 이미_담은_행사를_또_담아도_같은_응답이고_저장하지_않는다() throws Exception {
        login();
        when(favoriteRepository.existsByUserEmailAndEventId(EMAIL, "YI-0001")).thenReturn(true);

        mockMvc.perform(put("/api/me/favorites/YI-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));
        verify(favoriteRepository, never()).saveAndFlush(any(Favorite.class));
    }

    @Test
    void 해제는_favorited_false() throws Exception {
        login();

        mockMvc.perform(delete("/api/me/favorites/YI-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));
        verify(favoriteRepository).deleteByUserEmailAndEventId(EMAIL, "YI-0001");
    }

    @Test
    void 없는_행사는_E4041() throws Exception {
        login();

        mockMvc.perform(put("/api/me/favorites/YI-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E4041"));
        mockMvc.perform(delete("/api/me/favorites/YI-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("E4041"));
    }

    @Test
    void 로그인하면_목록과_상세의_isLiked가_담은_상태를_따른다() throws Exception {
        login();
        when(favoriteRepository.findEventIdsByUserEmail(EMAIL)).thenReturn(Set.of("YI-0001"));
        when(favoriteRepository.existsByUserEmailAndEventId(EMAIL, "YI-0001")).thenReturn(true);

        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17"))
                .andExpect(jsonPath("$.data.events[0].isLiked").value(true));
        mockMvc.perform(get("/api/events/YI-0001"))
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }

    @Test
    void 비로그인이면_isLiked는_false이고_관심_행사를_조회하지_않는다() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        mockMvc.perform(get("/api/events").param("dateFrom", "2026-10-17"))
                .andExpect(jsonPath("$.data.events[0].isLiked").value(false));
        mockMvc.perform(get("/api/events/YI-0001"))
                .andExpect(jsonPath("$.data.isLiked").value(false));
        verify(favoriteRepository, never()).findEventIdsByUserEmail(any());
    }

    @Test
    void 익명_인증은_사용자로_보지_않는다() {
        assertThat(SecurityUtil.getUsernameOrNull()).isNull();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(SecurityUtil.getUsernameOrNull()).isNull();
        login();
        assertThat(SecurityUtil.getUsernameOrNull()).isEqualTo(EMAIL);
    }
}
