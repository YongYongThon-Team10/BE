package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.response.RequestValueInvalidException;
import com.yongyongthon.be.common.util.Times;
import com.yongyongthon.be.itinerary.data.dto.ItineraryRequestDto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * 재추천(#9) 조건 검증. 보낸 항목만 검사하고, 실패하면 400 E4001.
 * 생성(#1)은 ItineraryCreateRequestDto의 애너테이션과 ItineraryRequestValidator가 검사한다.
 * 허용 값은 ItineraryCreateRequestDto의 패턴과 같다.
 */
public final class ItineraryConditionValidator {

    static final Set<String> ORIGIN_TYPES = Set.of("current", "manual");
    static final Set<String> TRANSPORTS = Set.of("car", "transit", "walk");
    static final Set<String> COMPANIONS = Set.of("alone", "couple", "friends", "family", "kids");
    static final Set<String> BUDGETS = Set.of("any", "free");
    static final Set<String> WANTS = Set.of("food", "experience", "rest", "performance");

    private ItineraryConditionValidator() {
    }

    /** 보낸 항목만 검사 */
    public static void validatePresent(ItineraryRequestDto r) {
        if (r.date() != null) {
            parseDate(r.date());
        }
        if (r.origin() != null) {
            validateOrigin(r.origin());
        }
        if (r.transport() != null && !TRANSPORTS.contains(r.transport())) {
            throw new RequestValueInvalidException();
        }
        if (r.timeWindow() != null) {
            validateWindow(r.timeWindow());
        }
        if (r.companion() != null && !COMPANIONS.contains(r.companion())) {
            throw new RequestValueInvalidException();
        }
        if (r.kidsAge() != null && r.kidsAge() < 0) {
            throw new RequestValueInvalidException();
        }
        if (r.budget() != null && !BUDGETS.contains(r.budget())) {
            throw new RequestValueInvalidException();
        }
        if (r.wants() != null) {
            for (String w : r.wants()) {
                if (w == null || !WANTS.contains(w)) {
                    throw new RequestValueInvalidException();
                }
            }
        }
    }

    public static LocalDate parseDate(String text) {
        if (text == null || !text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new RequestValueInvalidException();
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new RequestValueInvalidException();
        }
    }

    private static void validateOrigin(ItineraryRequestDto.Origin o) {
        if (o.type() == null || !ORIGIN_TYPES.contains(o.type()) || o.lat() == null || o.lng() == null
                || o.lat() < -90 || o.lat() > 90 || o.lng() < -180 || o.lng() > 180) {
            throw new RequestValueInvalidException();
        }
    }

    private static void validateWindow(ItineraryRequestDto.TimeWindow w) {
        if (!Times.isValid(w.start()) || !Times.isValid(w.end())
                || Times.toMinutes(w.end()) <= Times.toMinutes(w.start())) {
            throw new RequestValueInvalidException();
        }
    }
}
