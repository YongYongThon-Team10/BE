package com.yongyongthon.be.support;

import com.yongyongthon.be.event.data.entity.Event;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 API 테스트용 행사. ApiTestSupport가 테스트마다 events 테이블에 넣는다.
 * 값은 scripts/seed_events.py 적재 규칙을 따른다.
 * 1. category는 공연 또는 행사, subCategory는 원본 분류
 * 2. isFree는 요금에 무료가 있으면 true, 유료나 금액이 있으면 false, 모르면 null
 * 3. 원본의 중복 행은 적재하지 않으므로 넣지 않는다
 */
public final class TestEvents {

    private TestEvents() {
    }

    public static List<Event> all() {
        return List.of(
                event("T-BASE", "호수공원 가을 산책마켓", "행사", "축제",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), "10:00 ~ 18:00",
                        "기흥 호수공원 일대", "경기도 용인시 기흥구 테스트로 1", 37.2787, 127.117,
                        "무료", true),
                event("T-NEAR1", "근처 유료 공연", "공연", "공연",
                        LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-20"), null,
                        "근처 공연장", "경기도 용인시 기흥구 테스트로 2", 37.28, 127.12,
                        "유료 / 전석 5,000원", false),
                event("T-NEAR2", "근처 요금 미상 전시", "행사", "전시",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), "14:00 ~ 15:00",
                        "근처 전시관", "경기도 용인시 기흥구 테스트로 3", 37.285, 127.125,
                        null, null),
                event("T-NEAR3", "근처 무료 체험", "행사", "체험",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), "11:00 ~ 12:00",
                        "근처 체험관", "경기도 용인시 기흥구 테스트로 4", 37.29, 127.13,
                        "무료", true),
                event("T-FAR", "먼 행사", "행사", "축제",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), null,
                        "먼 곳", null, 37.4, 127.3,
                        null, null),
                event("T-NODATE", "기간 없는 행사", "행사", "축제",
                        null, null, null,
                        "근처", null, 37.279, 127.1172,
                        null, null),
                event("T-NOCOORD", "여러 도서관 순회 프로그램", "행사", "도서관 프로그램",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), null,
                        "여러 도서관", null, null, null,
                        null, null),
                event("T-NOTIME", "시간 미상 유료 행사", "공연", "공연",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), null,
                        "시간 미상 공연장", null, 37.26, 127.1,
                        "유료 / 전석 1만원", false),
                event("T-ALONE", "12월 외딴 행사", "행사", "축제",
                        LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-31"), "10:00 ~ 12:00",
                        "외딴 곳", null, 37.1, 127.4,
                        "무료", true),
                event("T-LIB", "근처 도서관 강좌", "행사", "도서관 프로그램",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), "10:00 ~ 12:00",
                        "근처 도서관", null, 37.2788, 127.1172,
                        null, null),
                event("T-RECRUIT", "근처 동아리 회원 모집", "행사", "체험",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), null,
                        "근처 동아리방", null, 37.2789, 127.1173,
                        null, null),
                event("T-SAMEDAY", "호수공원 가을 산책마켓 2부", "행사", "축제",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), "14:00 ~ 18:00",
                        "기흥 호수공원", null, 37.2787, 127.117,
                        "무료", true),
                event("T-LATE", "오후 공연", "공연", "공연",
                        LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"), "13:00 ~ 14:00",
                        "먼 공연장", null, 37.15, 127.35,
                        null, null)
        );
    }

    private static Event event(String id, String name, String category, String subCategory,
                               LocalDate startDate, LocalDate endDate, String time,
                               String placeName, String address, Double lat, Double lng,
                               String fee, Boolean isFree) {
        return Event.builder()
                .id(id)
                .name(name)
                .category(category)
                .subCategory(subCategory)
                .startDate(startDate)
                .endDate(endDate)
                .dateStatus(startDate == null ? "미확인" : "확정")
                .time(time)
                .placeName(placeName)
                .address(address)
                .lat(lat)
                .lng(lng)
                .coordPrecision(lat == null ? null : "place")
                .fee(fee)
                .isFree(isFree)
                .sourceUrl("https://example.com/events/" + id)
                .build();
    }
}
