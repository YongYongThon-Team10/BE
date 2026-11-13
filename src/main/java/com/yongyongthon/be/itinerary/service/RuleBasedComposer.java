package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.NearbyEvent;
import com.yongyongthon.be.itinerary.data.dto.WarningResponseDto;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.enums.SlotType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 규칙 기반 일정 구성 (지시서 6-1).
 * 1. event: 기준 행사
 * 2. meal: 빈 식사 슬롯
 * 3. nearbyEvent: 근처 행사 최대 2개. 가까운 후보부터 시간대 안에 들어가는 만큼만. 없으면 빈 슬롯 1개
 * 4. return: 귀가
 */
@Component
@RequiredArgsConstructor
public class RuleBasedComposer implements ItineraryComposer {

    private static final int MAX_NEARBY = 2;

    private final SlotFactory slotFactory;
    private final NearbyFinder nearbyFinder;
    private final Recalculator recalculator;

    @Override
    public void compose(Itinerary it, EventInfo base) {
        it.clearSlots();

        ItinerarySlot event = new ItinerarySlot(null, SlotType.EVENT);
        slotFactory.fillFromEvent(event, base, SlotType.EVENT);
        add(it, event, 1);

        ItinerarySlot meal = new ItinerarySlot(null, SlotType.MEAL);
        slotFactory.fillEmptyMeal(meal);
        add(it, meal, 2);

        ItinerarySlot ret = new ItinerarySlot(null, SlotType.RETURN);
        slotFactory.fillReturn(ret);
        add(it, ret, Integer.MAX_VALUE);

        int added = 0;
        double[] anchor = base.hasCoords() ? new double[]{base.lat(), base.lng()} : null;
        while (added < MAX_NEARBY && anchor != null) {
            List<NearbyEvent> candidates = nearbyFinder.find(it, base, anchor[0], anchor[1]);
            if (candidates.isEmpty()) {
                break;
            }
            EventInfo top = candidates.get(0).event();
            ItinerarySlot slot = new ItinerarySlot(null, SlotType.NEARBY_EVENT);
            slotFactory.fillFromEvent(slot, top, SlotType.NEARBY_EVENT);
            add(it, slot, 3 + added);
            if (exceedsWindow(it)) {
                it.removeSlot(slot);
                break;
            }
            added++;
            anchor = new double[]{top.lat(), top.lng()};
        }

        if (added == 0) {
            ItinerarySlot empty = new ItinerarySlot(null, SlotType.NEARBY_EVENT);
            slotFactory.fillEmptyNearby(empty);
            add(it, empty, 3);
        }

        // 순서가 정해진 뒤 slotId를 차례로 붙인다
        for (ItinerarySlot s : it.orderedSlots()) {
            s.setSlotId(it.nextSlotId());
        }
    }

    private boolean exceedsWindow(Itinerary it) {
        return recalculator.recalculate(it).stream()
                .anyMatch(w -> WarningResponseDto.TIME_EXCEEDED.equals(w.code()));
    }

    private static void add(Itinerary it, ItinerarySlot slot, int order) {
        slot.setSlotOrder(order);
        it.addSlot(slot);
    }
}
