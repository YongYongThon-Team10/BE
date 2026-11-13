package com.yongyongthon.be.event.data.repository;

/**
 * 원본 행사 데이터(엑셀, 행사 DB)를 EventInfo로 옮길 때의 규칙.
 * tools/convert_events.py와 같은 규칙이다. 원본에 없는 값은 만들지 않는다.
 */
public final class EventFieldRules {

    /** 원본 '중복 검토' 값 중 다른 행사의 중복 행을 뜻하는 값 */
    public static final String DUPLICATE_CONFIRMED = "중복 확인";

    private EventFieldRules() {
    }

    /** 앞뒤 공백을 지우고, 비면 null */
    public static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 요금 원문. 글자나 숫자가 하나도 없는 값('.', '-' 등)은 null */
    public static String fee(String raw) {
        String value = text(raw);
        if (value == null || !value.matches(".*[0-9A-Za-z가-힣].*")) {
            return null;
        }
        return value;
    }

    /** 요금에 '무료'가 있고 '유료'가 없을 때만 무료. 요금이 없으면 모름(false) */
    public static boolean isFree(String fee) {
        return fee != null && fee.contains("무료") && !fee.contains("유료");
    }

    /** 주소가 있으면 그 값, 없으면 확인된 위치 주소 */
    public static String address(String address, String verifiedAddress) {
        String a = text(address);
        return a != null ? a : text(verifiedAddress);
    }

    /** '중복 확인' 행이면 대표 행사 ID, 아니면 null */
    public static String duplicateOf(String duplicateStatus, String representativeId) {
        return DUPLICATE_CONFIRMED.equals(text(duplicateStatus)) ? text(representativeId) : null;
    }
}
