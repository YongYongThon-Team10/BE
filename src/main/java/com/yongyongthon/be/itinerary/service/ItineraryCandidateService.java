package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.util.Geo;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.NearbyEvent;
import com.yongyongthon.be.event.data.exception.EventNotFoundException;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.itinerary.data.dto.CandidateDto;
import com.yongyongthon.be.itinerary.data.dto.SlotCandidatesResponseDto;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.entity.SlotCandidate;
import com.yongyongthon.be.itinerary.data.enums.SlotType;
import com.yongyongthon.be.itinerary.data.exception.InvalidItineraryRequestException;
import com.yongyongthon.be.itinerary.data.repository.SlotCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 교체 후보 조회 (지시서 7장 #6 행사 후보, #7 식사 후보).
 */
@Service
@RequiredArgsConstructor
public class ItineraryCandidateService {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_EVENT_LIMIT = 10;
    private static final int MAX_MEAL_LIMIT = 5;
    private static final String MEAL_CATEGORY = "먹거리";

    private final ItineraryService itineraryService;
    private final EventRepository eventRepository;
    private final SlotCandidateRepository slotCandidateRepository;
    private final NearbyFinder nearbyFinder;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    /**
     * #6 행사 교체 후보. program, nearbyEvent, custom 슬롯에만 쓴다.
     * 반환한 후보는 slot_candidates에 저장해서(그 슬롯의 이전 후보는 덮어씀) PUT에서 찾는다.
     */
    @Transactional
    public GlobalResponse<SlotCandidatesResponseDto> getAlternatives(String itineraryId, String slotId, Integer limit) {
        int size = checkLimit(limit, MAX_EVENT_LIMIT);
        Itinerary it = itineraryService.getItineraryEntity(itineraryId);
        ItinerarySlot slot = ItinerarySlotService.getSlotEntity(it, slotId);
        String type = slot.getType();
        if (SlotType.MEAL.equals(type) || SlotType.EVENT.equals(type) || SlotType.RETURN.equals(type)) {
            throw new InvalidItineraryRequestException();
        }

        EventInfo base = eventRepository.findById(it.getBaseEventId()).orElseThrow(EventNotFoundException::new);
        double[] anchor = nearbyFinder.anchorBefore(it, slot, base);
        List<NearbyEvent> nearby = anchor == null
                ? List.of()
                : nearbyFinder.find(it, base, anchor[0], anchor[1]);

        double speed = props.speedOf(it.getTransport());
        List<CandidateDto> candidates = new ArrayList<>();
        for (NearbyEvent n : nearby.stream().limit(size).toList()) {
            candidates.add(toCandidate("cnd_" + (candidates.size() + 1), n, speed));
        }

        slotCandidateRepository.deleteBySlot(itineraryId, slotId);
        for (CandidateDto c : candidates) {
            slotCandidateRepository.save(SlotCandidate.builder()
                    .itineraryId(itineraryId)
                    .slotId(slotId)
                    .candidateId(c.candidateId())
                    .eventId(c.eventId())
                    .payload(objectMapper.writeValueAsString(c))
                    .build());
        }
        return GlobalResponse.ok(new SlotCandidatesResponseDto(slotId, type, candidates));
    }

    /** #7 식사 후보. meal 슬롯에만 쓴다. 샘플 5개를 앞에서부터 limit개 */
    @Transactional(readOnly = true)
    public GlobalResponse<SlotCandidatesResponseDto> getMealCandidates(String itineraryId, String slotId, Integer limit) {
        int size = checkLimit(limit, MAX_MEAL_LIMIT);
        Itinerary it = itineraryService.getItineraryEntity(itineraryId);
        ItinerarySlot slot = ItinerarySlotService.getSlotEntity(it, slotId);
        if (!SlotType.MEAL.equals(slot.getType())) {
            throw new InvalidItineraryRequestException();
        }
        List<CandidateDto> candidates = MealSamples.ALL.stream()
                .limit(size)
                .map(m -> new CandidateDto(
                        m.candidateId(), "meal", null, m.name(), MEAL_CATEGORY, m.name(),
                        null, null, null, m.durationMin(), m.costAmount(), m.costText(),
                        m.distanceKm(), null, m.description(), "sample"))
                .toList();
        return GlobalResponse.ok(new SlotCandidatesResponseDto(slotId, slot.getType(), candidates));
    }

    private CandidateDto toCandidate(String candidateId, NearbyEvent n, double speed) {
        EventInfo e = n.event();
        CostRule.Cost cost = CostRule.ofEvent(e.isFree(), e.fee());
        int duration = e.runningTimeMin() != null ? e.runningTimeMin() : props.getNearbyEventDurationMin();
        return new CandidateDto(
                candidateId, "event", e.eventId(), e.name(), e.category(), e.placeName(),
                e.lat(), e.lng(), e.time(), duration, cost.amount(), cost.text(),
                Geo.round1(n.distanceKm()), Geo.estimateMinutes(n.distanceKm(), speed),
                e.summary(), "event");
    }

    private static int checkLimit(Integer limit, int max) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > max) {
            throw new InvalidItineraryRequestException();
        }
        return limit;
    }
}
