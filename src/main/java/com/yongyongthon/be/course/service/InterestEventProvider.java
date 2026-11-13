package com.yongyongthon.be.course.service;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 관심 행사(event_likes) ID 목록. 관심 행사 담기(S3-11)는 다른 팀원 담당이라
 * 그 기능이 생기기 전까지는 빈 목록을 돌려준다 (지시서 7장 #15, 11장 #12).
 */
@Component
public class InterestEventProvider {

    public Set<String> likedEventIds() {
        return Set.of();
    }
}
