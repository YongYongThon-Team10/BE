package com.yongyongthon.be.itinerary;

import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.common.util.Times;
import com.yongyongthon.be.itinerary.service.CostRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 9장 3단계: 비용(6-3), 이동시간(6-4), 시간 구간(6-2 #7) 규칙 단위 테스트.
 */
class CalculationRuleTest {

    @Test
    void 무료_행사는_0원_무료() {
        CostRule.Cost cost = CostRule.ofEvent(true, "전석무료");
        assertThat(cost.amount()).isZero();
        assertThat(cost.text()).isEqualTo("무료");
    }

    @Test
    void 요금의_첫_원_단위_금액을_읽는다() {
        assertThat(CostRule.ofEvent(false, "5,000원").amount()).isEqualTo(5000);
        assertThat(CostRule.ofEvent(false, "유료 / 1층 20000원 + 2층 15000원").amount()).isEqualTo(20000);
        assertThat(CostRule.ofEvent(false, "유료 / 전석 1만원").amount()).isEqualTo(10000);
        assertThat(CostRule.ofEvent(false, "유료 / VIP석 3만원 + R석 2만5천원").amount()).isEqualTo(30000);
        assertThat(CostRule.parseWon("R석 2만5천원")).isEqualTo(25000);
        assertThat(CostRule.ofEvent(false, "유료 / 전석 1만원").text()).isEqualTo("유료 / 전석 1만원");
    }

    @Test
    void 숫자를_읽을_수_없으면_금액만_null() {
        CostRule.Cost cost = CostRule.ofEvent(false, "프로그램에 따라 상이");
        assertThat(cost.amount()).isNull();
        assertThat(cost.text()).isEqualTo("프로그램에 따라 상이");
    }

    @Test
    void 요금이_없으면_둘_다_null() {
        CostRule.Cost cost = CostRule.ofEvent(false, null);
        assertThat(cost.amount()).isNull();
        assertThat(cost.text()).isNull();
    }

    @Test
    void 직접_입력_비용() {
        assertThat(CostRule.ofCustom("12,000원").amount()).isEqualTo(12000);
        assertThat(CostRule.ofCustom("무료").amount()).isZero();
        assertThat(CostRule.ofCustom("적당히").amount()).isNull();
    }

    @Test
    void 이동시간은_5분_단위_올림_최소_5분() {
        assertThat(Geo.estimateMinutes(0.1, 30)).isEqualTo(5);
        assertThat(Geo.estimateMinutes(4.1, 30)).isEqualTo(10);
        assertThat(Geo.estimateMinutes(5.0, 30)).isEqualTo(10);
        assertThat(Geo.estimateMinutes(5.1, 30)).isEqualTo(15);
        assertThat(Geo.estimateMinutes(1.0, 4)).isEqualTo(15);
    }

    @Test
    void 직선거리_계산() {
        double km = Geo.haversineKm(37.2756, 127.1163, 37.2787, 127.117);
        assertThat(km).isBetween(0.3, 0.4);
        assertThat(Geo.round1(4.06)).isEqualTo(4.1);
    }

    @Test
    void 행사_시간_한_구간_읽기() {
        assertThat(Times.parseRange("10:00 ~ 12:00")).containsExactly(600, 720);
        assertThat(Times.parseRange("10:00~12:00")).containsExactly(600, 720);
        assertThat(Times.parseRange("00:00 ~ 00:00")).isNull();
        assertThat(Times.parseRange("10:00")).isNull();
        assertThat(Times.parseRange(null)).isNull();
        assertThat(Times.format(24 * 60 + 10)).isEqualTo("24:10");
    }
}
