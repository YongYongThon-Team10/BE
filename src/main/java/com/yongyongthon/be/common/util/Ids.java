package com.yongyongthon.be.common.util;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 추측하기 어려운 ID 생성 (지시서 3-2).
 */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static String random(String prefix, int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return prefix + HexFormat.of().formatHex(buf);
    }
}
