package com.yongyongthon.be.search.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * E4에서 모델이 자주 틀리는 값(상대 날짜, 동의 소속 구)을 서버가 정하는 규칙.
 */
final class SearchConditionRules {

    private SearchConditionRules() {
    }

    /** 기간 [from, to] */
    record DateRange(LocalDate from, LocalDate to) {
    }

    /**
     * 상대 날짜 표현과 그 기간. 긴 표현부터 찾도록 순서를 지킨다 (다음 주말을 다음 주로 읽지 않게).
     * 이번 주말은 오늘이 토요일이면 오늘과 내일, 일요일이면 오늘만, 그 외에는 다가오는 토요일과 일요일이다.
     * 이번 주는 오늘부터 이번 일요일까지, 다음 주는 다음 월요일부터 일요일까지다.
     */
    static Map<String, DateRange> relativeDates(LocalDate today) {
        LocalDate thisSaturday = today.getDayOfWeek() == DayOfWeek.SUNDAY
                ? today.minusDays(1)
                : today.plusDays(DayOfWeek.SATURDAY.getValue() - today.getDayOfWeek().getValue());
        LocalDate thisSunday = thisSaturday.plusDays(1);
        LocalDate nextMonday = thisSunday.plusDays(1);

        Map<String, DateRange> ranges = new LinkedHashMap<>();
        ranges.put("다음주말", new DateRange(thisSaturday.plusDays(7), thisSunday.plusDays(7)));
        ranges.put("이번주말", new DateRange(today.isAfter(thisSaturday) ? today : thisSaturday, thisSunday));
        ranges.put("주말", new DateRange(today.isAfter(thisSaturday) ? today : thisSaturday, thisSunday));
        ranges.put("다음주", new DateRange(nextMonday, nextMonday.plusDays(6)));
        ranges.put("이번주", new DateRange(today, thisSunday));
        ranges.put("모레", new DateRange(today.plusDays(2), today.plusDays(2)));
        ranges.put("내일", new DateRange(today.plusDays(1), today.plusDays(1)));
        ranges.put("오늘", new DateRange(today, today));
        return ranges;
    }

    /** 문장(공백 무시)에 들어 있는 첫 상대 날짜 표현의 기간. 없으면 null */
    static DateRange findRelativeDate(String query, LocalDate today) {
        String compact = query.replaceAll("\\s+", "");
        for (Map.Entry<String, DateRange> entry : relativeDates(today).entrySet()) {
            if (compact.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 용인시 동, 읍, 면과 소속 구. 행정동(신갈동, 동백1동)과 법정동(마북동, 보정동), 번호를 뗀 이름(동백동)을 모두 넣었다.
     * 출처: 위키백과 "용인시의 행정 구역" (2026-10-15 판, 3구 5읍 2면 32동). 행정 구역이 바뀌면 고친다.
     */
    static final Map<String, String> AREA_TO_DISTRICT = buildAreas();

    private static Map<String, String> buildAreas() {
        Map<String, String> areas = new LinkedHashMap<>();
        put(areas, "처인구", List.of(
                "중앙동", "역북동", "삼가동", "유림동", "유림1동", "유림2동", "동부동",
                "김량장동", "남동", "유방동", "고림동", "마평동", "운학동", "호동", "해곡동",
                "포곡읍", "모현읍", "이동읍", "남사읍", "양지읍", "원삼면", "백암면"));
        put(areas, "기흥구", List.of(
                "신갈동", "영덕동", "영덕1동", "영덕2동", "구갈동", "상갈동", "보라동", "기흥동", "서농동", "구성동",
                "마북동", "동백동", "동백1동", "동백2동", "동백3동", "상하동", "보정동",
                "하갈동", "지곡동", "공세동", "고매동", "서천동", "농서동", "언남동", "청덕동", "중동"));
        put(areas, "수지구", List.of(
                "풍덕천동", "풍덕천1동", "풍덕천2동", "신봉동", "죽전동", "죽전1동", "죽전2동", "죽전3동",
                "동천동", "상현동", "상현1동", "상현2동", "상현3동", "성복동", "고기동"));
        return Map.copyOf(areas);
    }

    private static void put(Map<String, String> areas, String district, List<String> names) {
        names.forEach(name -> areas.put(name, district));
    }

    /** 구 이름이 문장에 있는지. "수지구"뿐 아니라 "수지"만 말해도 있다고 본다 */
    static boolean mentionsDistrict(String query, String district) {
        return query.contains(district.substring(0, district.length() - 1));
    }

    /** 앞글자가 흔한 낱말이라 전체 이름으로만 찾는 지명 (예: "이동 거리"를 이동읍으로 읽지 않게) */
    private static final java.util.Set<String> COMMON_WORD_STEMS = java.util.Set.of("이동", "구성", "중앙", "동부", "남사");

    /**
     * 동, 읍, 면 이름이 문장에 있는지. "동백동"은 "동백"만 말해도 있다고 본다.
     * "남동", "중동", "호동"처럼 앞글자가 한 글자이거나 흔한 낱말인 이름은 전체 이름이 있어야 한다.
     */
    static boolean mentionsArea(String query, String area) {
        String compact = query.replaceAll("\\s+", "");
        String stem = area.replaceAll("\\d*[동읍면]$", "");
        return compact.contains(area)
                || (stem.length() >= 2 && !COMMON_WORD_STEMS.contains(stem) && compact.contains(stem));
    }
}
