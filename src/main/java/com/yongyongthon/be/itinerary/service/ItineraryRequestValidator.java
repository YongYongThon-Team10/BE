package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.response.RequestValueInvalidException;
import com.yongyongthon.be.event.data.entity.Event;
import com.yongyongthon.be.event.service.EventService;
import com.yongyongthon.be.itinerary.data.dto.ItineraryCreateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * E5 요청 중 애너테이션으로 못 잡는 규칙을 검사한다. S5의 일정 생성 서비스가 생성 전에 호출한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItineraryRequestValidator {

    private final EventService eventService;

    /**
     * @return 기준 행사. 없으면 404 E4041, 시간대가 역전이면 400 E4001
     */
    public Event validate(ItineraryCreateRequestDto request) {
        ItineraryCreateRequestDto.TimeWindow window = request.getTimeWindow();
        if (!window.getEnd().isAfter(window.getStart())) {
            log.warn("[validate] 시간대 역전: {} ~ {}", window.getStart(), window.getEnd());
            throw new RequestValueInvalidException();
        }
        return eventService.getEventEntity(request.getBaseEventId());
    }
}
