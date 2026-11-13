package com.yongyongthon.be.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HH:mm 문자열과 분(0시 기준) 변환.
 * 일정이 자정을 넘으면 자르지 않고 24:10처럼 그대로 표기한다.
 */
public final class Times {

    private static final Pattern HH_MM = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");
    private static final Pattern RANGE = Pattern.compile(
            "^\\s*([01]\\d|2[0-3]):([0-5]\\d)\\s*~\\s*([01]\\d|2[0-3]):([0-5]\\d)\\s*$");

    private Times() {
    }

    public static boolean isValid(String hhmm) {
        return hhmm != null && HH_MM.matcher(hhmm).matches();
    }

    /** "HH:mm"(24 이상 시각 포함)을 분으로 */
    public static int toMinutes(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public static String format(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    /**
     * 행사 time이 "HH:mm ~ HH:mm" 한 구간이면 [시작분, 종료분], 아니면 null.
     * 종료가 시작보다 늦어야 한 구간으로 본다.
     */
    public static int[] parseRange(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = RANGE.matcher(text);
        if (!m.matches()) {
            return null;
        }
        int start = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        int end = Integer.parseInt(m.group(3)) * 60 + Integer.parseInt(m.group(4));
        return end > start ? new int[]{start, end} : null;
    }
}
