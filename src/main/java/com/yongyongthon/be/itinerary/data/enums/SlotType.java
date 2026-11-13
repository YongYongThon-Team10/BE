package com.yongyongthon.be.itinerary.data.enums;

import java.util.Set;

/**
 * 슬롯 type 값 (지시서 4-2). 응답에 문자열 그대로 나가므로 상수로 둔다.
 */
public final class SlotType {

    public static final String EVENT = "event";
    public static final String PROGRAM = "program";
    public static final String MEAL = "meal";
    public static final String NEARBY_EVENT = "nearbyEvent";
    public static final String RETURN = "return";
    public static final String CUSTOM = "custom";

    /** 행사 DB에서 온 슬롯 */
    public static final Set<String> FROM_EVENT_DB = Set.of(EVENT, NEARBY_EVENT, PROGRAM);

    private SlotType() {
    }
}
