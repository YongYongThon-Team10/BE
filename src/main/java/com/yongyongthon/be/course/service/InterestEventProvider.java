package com.yongyongthon.be.course.service;

import com.yongyongthon.be.common.security.SecurityUtil;
import com.yongyongthon.be.event.data.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 캘린더에 보일 관심 행사 ID 목록 (지시서 7장 #15). 관심 행사는 favorites 테이블(E19, E20)이다.
 * 캘린더는 로그인 없이도 열리므로, 토큰이 있으면 그 사용자의 관심 행사를, 없으면 빈 목록을 돌려준다.
 */
@Component
@RequiredArgsConstructor
public class InterestEventProvider {

    private final FavoriteRepository favoriteRepository;

    public Set<String> likedEventIds() {
        String email = SecurityUtil.getUsernameOrNull();
        if (email == null) {
            return Set.of();
        }
        return favoriteRepository.findEventIdsByUserEmail(email);
    }
}
