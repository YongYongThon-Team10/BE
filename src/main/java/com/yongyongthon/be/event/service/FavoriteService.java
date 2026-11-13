package com.yongyongthon.be.event.service;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.event.data.dto.FavoriteResponseDto;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.data.entity.Favorite;
import com.yongyongthon.be.event.data.repository.FavoriteRepository;
import com.yongyongthon.be.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 관심 행사 담기와 해제. 둘 다 멱등이라 여러 번 호출해도 같은 응답을 준다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final EventService eventService;
    private final AuthService authService;

    // 트랜잭션을 걸지 않는다. 걸면 유니크 제약 위반 시 트랜잭션 전체가 롤백 전용이 되어 아래 catch가 소용없다
    public GlobalResponse<FavoriteResponseDto> addFavorite(String email, String eventId) {
        Event event = eventService.getEventEntity(eventId);

        if (!favoriteRepository.existsByUserEmailAndEventId(email, eventId)) {
            try {
                favoriteRepository.saveAndFlush(Favorite.builder()
                        .user(authService.getUserEntity(email))
                        .event(event)
                        .createdAt(LocalDateTime.now())
                        .build());
                log.info("[addFavorite] 관심 행사 담기: {}, {}", email, eventId);
            } catch (DataIntegrityViolationException e) {
                // 같은 요청이 동시에 들어와 유니크 제약에 걸린 경우. 이미 담긴 상태이므로 성공으로 본다
                log.info("[addFavorite] 이미 담긴 관심 행사: {}, {}", email, eventId);
            }
        }

        return GlobalResponse.ok(FavoriteResponseDto.builder().eventId(eventId).favorited(true).build());
    }

    @Transactional
    public GlobalResponse<FavoriteResponseDto> removeFavorite(String email, String eventId) {
        eventService.getEventEntity(eventId);

        long deleted = favoriteRepository.deleteByUserEmailAndEventId(email, eventId);
        log.info("[removeFavorite] 관심 행사 해제: {}, {} (삭제 {}건)", email, eventId, deleted);

        return GlobalResponse.ok(FavoriteResponseDto.builder().eventId(eventId).favorited(false).build());
    }
}
