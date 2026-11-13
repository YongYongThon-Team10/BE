package com.yongyongthon.be.common.util;

/**
 * 직선거리와 이동시간 추정 (지시서 6-4).
 */
public final class Geo {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private Geo() {
    }

    /** 하버사인 직선거리(km) */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 소수 첫째 자리 반올림 */
    public static double round1(double km) {
        return Math.round(km * 10) / 10.0;
    }

    /** max(5, ceil(거리 / 속도 x 60 / 5) x 5) */
    public static int estimateMinutes(double km, double speedKmh) {
        int rounded = (int) Math.ceil(km / speedKmh * 60 / 5) * 5;
        return Math.max(5, rounded);
    }
}
