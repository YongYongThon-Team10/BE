package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;

/**
 * 일정 슬롯 구성 (지시서 6-6). 지금은 규칙 기반(RuleBasedComposer)만 있다.
 * LLM 구현을 붙이더라도 후보 중에서 고르기, 제목과 설명 문구 쓰기만 할 수 있다.
 */
public interface ItineraryComposer {

    /** it의 조건(날짜, 출발지, 시간대, 예산 등)으로 슬롯을 처음부터 다시 만든다 */
    void compose(Itinerary it, EventInfo base);
}
