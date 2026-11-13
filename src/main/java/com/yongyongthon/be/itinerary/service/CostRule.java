package com.yongyongthon.be.itinerary.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 비용 규칙 (지시서 6-3). 비용을 추측해서 채우지 않는다.
 */
public final class CostRule {

    /**
     * '원' 단위 금액의 첫 번째 값. 예: "5,000원", "1만원", "2만5천원", "2천원".
     * 원본 데이터에 "1만원" 같은 표기가 있어 만, 천 단위도 읽는다.
     */
    private static final Pattern WON = Pattern.compile(
            "(\\d[\\d,]*)\\s*(?:(만)\\s*(?:(\\d+)\\s*천)?|(천))?\\s*원");

    private CostRule() {
    }

    public record Cost(Integer amount, String text) {
        static final Cost UNKNOWN = new Cost(null, null);
        static final Cost FREE = new Cost(0, "무료");
    }

    /** 행사, 프로그램: isFree면 무료, 아니면 fee 원문과 첫 금액 */
    public static Cost ofEvent(Boolean isFree, String fee) {
        if (Boolean.TRUE.equals(isFree)) {
            return Cost.FREE;
        }
        if (fee == null || fee.isBlank()) {
            return Cost.UNKNOWN;
        }
        return new Cost(parseWon(fee), fee);
    }

    /** 직접 입력(custom): 입력 원문, 같은 규칙으로 읽고 못 읽으면 null. 숫자 없이 '무료'만 있으면 0 */
    public static Cost ofCustom(String costText) {
        if (costText == null || costText.isBlank()) {
            return Cost.UNKNOWN;
        }
        Integer amount = parseWon(costText);
        if (amount == null && costText.contains("무료") && !costText.matches(".*\\d.*")) {
            amount = 0;
        }
        return new Cost(amount, costText);
    }

    public static Integer parseWon(String text) {
        Matcher m = WON.matcher(text);
        if (!m.find()) {
            return null;
        }
        try {
            long value = Long.parseLong(m.group(1).replace(",", ""));
            if (m.group(2) != null) {
                value *= 10_000;
                if (m.group(3) != null) {
                    value += Long.parseLong(m.group(3)) * 1_000;
                }
            } else if (m.group(4) != null) {
                value *= 1_000;
            }
            return value > Integer.MAX_VALUE ? null : (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
