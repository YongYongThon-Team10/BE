package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.EventProgram;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.enums.SlotType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 슬롯 내용 채우기 (지시서 6-1 기본 머무는 시간, 6-3 비용).
 */
@Component
@RequiredArgsConstructor
public class SlotFactory {

    public static final String MEAL_TITLE = "식사";

    private final AppProperties props;

    /** 기준 행사(event) 또는 근처 행사(nearbyEvent) */
    public void fillFromEvent(ItinerarySlot slot, EventInfo e, String type) {
        slot.clearContent();
        slot.setType(type);
        slot.setTitle(e.name());
        slot.setEventId(e.eventId());
        int fallback = SlotType.EVENT.equals(type) ? props.getEventDurationMin() : props.getNearbyEventDurationMin();
        slot.setDurationMin(e.runningTimeMin() != null ? e.runningTimeMin() : fallback);
        CostRule.Cost cost = CostRule.ofEvent(e.isFree(), e.fee());
        slot.setCostAmount(cost.amount());
        slot.setCostText(cost.text());
        slot.setDescription(e.summary());
        setEventPlace(slot, e);
    }

    /** 행사 안 프로그램 (지시서 7장 #3) */
    public void fillFromProgram(ItinerarySlot slot, EventInfo e, EventProgram p) {
        slot.clearContent();
        slot.setType(SlotType.PROGRAM);
        slot.setTitle(p.name());
        slot.setEventId(e.eventId());
        slot.setProgramId(p.programId());
        slot.setDurationMin(p.durationMin() != null ? p.durationMin() : props.getProgramDurationMin());
        CostRule.Cost cost = CostRule.ofEvent(p.isFree(), p.fee());
        slot.setCostAmount(cost.amount());
        slot.setCostText(cost.text());
        slot.setDescription(p.summary());
        setEventPlace(slot, e);
    }

    /** 빈 식사 슬롯 (지시서 6-1): title="식사", place=null, costAmount=null, isUserAdded=false */
    public void fillEmptyMeal(ItinerarySlot slot) {
        slot.clearContent();
        slot.setType(SlotType.MEAL);
        slot.setTitle(MEAL_TITLE);
        slot.setDurationMin(props.getMealDurationMin());
        slot.setUserAdded(false);
    }

    /** 사용자가 입력한 식사. 비용은 항상 null(미확인) */
    public void fillUserMeal(ItinerarySlot slot, String title, Integer durationMin, String address) {
        slot.clearContent();
        slot.setType(SlotType.MEAL);
        slot.setTitle(title);
        slot.setDurationMin(durationMin != null ? durationMin : props.getMealDurationMin());
        slot.setUserAdded(true);
        if (address != null) {
            slot.setPlace(title, address, null, null);
        }
    }

    /** 직접 입력 */
    public void fillCustom(ItinerarySlot slot, String title, Integer durationMin, String costText, String address) {
        slot.clearContent();
        slot.setType(SlotType.CUSTOM);
        slot.setTitle(title);
        slot.setDurationMin(durationMin != null ? durationMin : props.getCustomDurationMin());
        CostRule.Cost cost = CostRule.ofCustom(costText);
        slot.setCostAmount(cost.amount());
        slot.setCostText(cost.text());
        slot.setUserAdded(true);
        if (address != null) {
            slot.setPlace(title, address, null, null);
        }
    }

    /** 식사 샘플 후보로 교체 (지시서 7장 #5): place는 null, isUserAdded=false */
    public void fillMealSample(ItinerarySlot slot, MealSamples.MealSample m) {
        slot.clearContent();
        slot.setType(SlotType.MEAL);
        slot.setTitle(m.name());
        slot.setDurationMin(m.durationMin());
        slot.setCostAmount(m.costAmount());
        slot.setCostText(m.costText());
        slot.setDescription(m.description());
        slot.setUserAdded(false);
    }

    /** 일정을 만들 때 둔 빈 식사 슬롯인지 (사용자가 채우거나 교체하지 않음) */
    public static boolean isEmptyMeal(ItinerarySlot slot) {
        return SlotType.MEAL.equals(slot.getType()) && !slot.isUserAdded()
                && MEAL_TITLE.equals(slot.getTitle()) && slot.getCostText() == null;
    }

    /** 후보가 없을 때 두는 빈 근처 행사 슬롯 */
    public void fillEmptyNearby(ItinerarySlot slot) {
        slot.clearContent();
        slot.setType(SlotType.NEARBY_EVENT);
    }

    public void fillReturn(ItinerarySlot slot) {
        slot.clearContent();
        slot.setType(SlotType.RETURN);
        slot.setTitle(Recalculator.RETURN_TITLE);
    }

    private void setEventPlace(ItinerarySlot slot, EventInfo e) {
        if (e.placeName() != null || e.address() != null || e.hasCoords()) {
            slot.setPlace(e.placeName(), e.address(), e.lat(), e.lng());
        }
    }
}
