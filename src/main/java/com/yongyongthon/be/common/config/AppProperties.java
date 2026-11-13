package com.yongyongthon.be.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 일정 기능 설정값. 임시 값은 S5, S6 지시서 11장 가정과 같다.
 * 모두 기본값이 있어 application.yml에 적지 않아도 동작한다.
 */
@Getter
@Component
public class AppProperties {

    /** 공유 링크 기본 주소 (지시서 11장 #10, 배포 주소 미정) */
    @Value("${app.share-base-url:https://example.com/shared}")
    private String shareBaseUrl;

    /** 근처 행사 반경 km (지시서 6-5) */
    @Value("${app.nearby-radius-km:5.0}")
    private double nearbyRadiusKm;

    /** 이동수단별 속도 km/h (지시서 6-4) */
    @Value("${app.speed-kmh.car:30}")
    private double carSpeedKmh;

    @Value("${app.speed-kmh.transit:20}")
    private double transitSpeedKmh;

    @Value("${app.speed-kmh.walk:4}")
    private double walkSpeedKmh;

    /** 기본 머무는 시간 분 (지시서 6-1) */
    @Value("${app.default-duration-min.event:60}")
    private int eventDurationMin;

    @Value("${app.default-duration-min.program:60}")
    private int programDurationMin;

    @Value("${app.default-duration-min.meal:45}")
    private int mealDurationMin;

    @Value("${app.default-duration-min.nearby-event:60}")
    private int nearbyEventDurationMin;

    @Value("${app.default-duration-min.custom:30}")
    private int customDurationMin;

    /** 근처 행사 후보에서 뺄 분류 (콤마 구분). 수강 신청이 필요한 강좌라 바로 가는 행사로 맞지 않음 */
    @Value("${app.nearby-excluded-categories:도서관 프로그램}")
    private String nearbyExcludedCategories;

    /** 이름에 이 단어가 있으면 근처 행사 후보에서 뺀다 (콤마 구분). 예: 회원 모집 공지 */
    @Value("${app.nearby-excluded-keywords:모집}")
    private String nearbyExcludedKeywords;

    /** 행사 데이터 출처: json(엑셀 변환 파일) 또는 postgres(팀 행사 DB) */
    @Value("${app.event-data.source:json}")
    private String eventSource;

    /** 행사 DB 접속 정보 (postgres 모드). 비밀번호는 환경변수 EVENT_DB_PASSWORD로만 받는다 */
    @Value("${app.event-data.db-url:jdbc:postgresql://127.0.0.1:15432/yongin_events?sslmode=require}")
    private String eventDbUrl;

    @Value("${app.event-data.db-username:yongin_reader}")
    private String eventDbUsername;

    @lombok.Getter(lombok.AccessLevel.NONE)
    @Value("${app.event-data.db-password:}")
    private String eventDbPassword;

    /** 행사 데이터 JSON 위치 (지시서 8장) */
    @Value("${app.event-data.events-path:classpath:data/events_all.json}")
    private String eventsPath;

    public java.util.List<String> nearbyExcludedCategoryList() {
        return splitCsv(nearbyExcludedCategories);
    }

    public java.util.List<String> nearbyExcludedKeywordList() {
        return splitCsv(nearbyExcludedKeywords);
    }

    private static java.util.List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
    }

    /** toString 등으로 새지 않게 getter를 따로 둔다 */
    public String eventDbPassword() {
        return eventDbPassword;
    }

    public double speedOf(String transport) {
        if ("transit".equals(transport)) {
            return transitSpeedKmh;
        }
        if ("walk".equals(transport)) {
            return walkSpeedKmh;
        }
        return carSpeedKmh;
    }
}
