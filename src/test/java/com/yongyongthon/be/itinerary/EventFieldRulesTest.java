package com.yongyongthon.be.itinerary;

import com.yongyongthon.be.event.data.repository.EventFieldRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 행사 DB 행을 EventInfo로 옮기는 규칙 (tools/convert_events.py와 같은 규칙).
 */
class EventFieldRulesTest {

    @Test
    void 요금_정리() {
        assertThat(EventFieldRules.fee(" 유료 / 전석 1만원 ")).isEqualTo("유료 / 전석 1만원");
        assertThat(EventFieldRules.fee(".")).isNull();
        assertThat(EventFieldRules.fee("'-")).isNull();
        assertThat(EventFieldRules.fee(null)).isNull();
    }

    @Test
    void 무료_판단() {
        assertThat(EventFieldRules.isFree("전석무료(네이버예약)")).isTrue();
        assertThat(EventFieldRules.isFree("무료 공연")).isTrue();
        assertThat(EventFieldRules.isFree("유료 / 무료 좌석 일부")).isFalse();
        assertThat(EventFieldRules.isFree(null)).isFalse();
    }

    @Test
    void 주소는_원주소_우선() {
        assertThat(EventFieldRules.address("경기도 용인시 1", "경기 용인시 2")).isEqualTo("경기도 용인시 1");
        assertThat(EventFieldRules.address("  ", "경기 용인시 2")).isEqualTo("경기 용인시 2");
    }

    @Test
    void 중복_확인_행만_대표_ID() {
        assertThat(EventFieldRules.duplicateOf("중복 확인", "YI-1053")).isEqualTo("YI-1053");
        assertThat(EventFieldRules.duplicateOf("대표 행", "YI-1053")).isNull();
        assertThat(EventFieldRules.duplicateOf(null, null)).isNull();
    }
}
