package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.util.Times;
import com.yongyongthon.be.itinerary.data.dto.ItineraryRequestDto;
import com.yongyongthon.be.itinerary.data.exception.InvalidItineraryRequestException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * 일정 입력 검증 (지시서 7장 #1). 실패하면 400 C001.
 */
public final class ItineraryRequestValidator {

    static final Set<String> ORIGIN_TYPES = Set.of("current", "manual");
    static final Set<String> TRANSPORTS = Set.of("car", "transit", "walk");
    static final Set<String> COMPANIONS = Set.of("alone", "couple", "friends", "family", "kids");
    static final Set<String> BUDGETS = Set.of("any", "free");
    static final Set<String> WANTS = Set.of("food", "experience", "rest", "performance");

    private ItineraryRequestValidator() {
    }

    /** 생성 요청: 필수값을 모두 검사 */
    public static void validateCreate(ItineraryRequestDto r) {
        if (r == null || r.baseEventId() == null || r.baseEventId().isBlank()
                || r.date() == null || r.origin() == null || r.transport() == null || r.timeWindow() == null) {
            throw new InvalidItineraryRequestException();
        }
        validatePresent(r);
    }

    /** 보낸 항목만 검사 (재추천 공통) */
    public static void validatePresent(ItineraryRequestDto r) {
        if (r.date() != null) {
            parseDate(r.date());
        }
        if (r.origin() != null) {
            validateOrigin(r.origin());
        }
        if (r.transport() != null && !TRANSPORTS.contains(r.transport())) {
            throw new InvalidItineraryRequestException();
        }
        if (r.timeWindow() != null) {
            validateWindow(r.timeWindow());
        }
        if (r.companion() != null && !COMPANIONS.contains(r.companion())) {
            throw new InvalidItineraryRequestException();
        }
        if (r.kidsAge() != null && (r.kidsAge() < 0 || r.kidsAge() > 18)) {
            throw new InvalidItineraryRequestException();
        }
        if (r.budget() != null && !BUDGETS.contains(r.budget())) {
            throw new InvalidItineraryRequestException();
        }
        if (r.wants() != null) {
            for (String w : r.wants()) {
                if (w == null || !WANTS.contains(w)) {
                    throw new InvalidItineraryRequestException();
                }
            }
        }
    }

    public static LocalDate parseDate(String text) {
        if (text == null || !text.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidItineraryRequestException();
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidItineraryRequestException();
        }
    }

    private static void validateOrigin(ItineraryRequestDto.Origin o) {
        if (o.type() == null || !ORIGIN_TYPES.contains(o.type()) || o.lat() == null || o.lng() == null
                || o.lat() < -90 || o.lat() > 90 || o.lng() < -180 || o.lng() > 180) {
            throw new InvalidItineraryRequestException();
        }
    }

    private static void validateWindow(ItineraryRequestDto.TimeWindow w) {
        if (!Times.isValid(w.start()) || !Times.isValid(w.end())
                || Times.toMinutes(w.end()) <= Times.toMinutes(w.start())) {
            throw new InvalidItineraryRequestException();
        }
    }
}
