package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.dto.EventProgram;
import com.yongyongthon.be.event.data.exception.EventNotFoundException;
import com.yongyongthon.be.event.data.repository.EventRepository;
import com.yongyongthon.be.itinerary.data.dto.ItineraryResponseDto;
import com.yongyongthon.be.itinerary.data.dto.SlotAddRequestDto;
import com.yongyongthon.be.itinerary.data.dto.SlotOrderRequestDto;
import com.yongyongthon.be.itinerary.data.dto.SlotReplaceRequestDto;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.entity.SlotCandidate;
import com.yongyongthon.be.itinerary.data.enums.SlotType;
import com.yongyongthon.be.itinerary.data.exception.InvalidItineraryRequestException;
import com.yongyongthon.be.itinerary.data.exception.SlotNotDeletableException;
import com.yongyongthon.be.itinerary.data.exception.SlotNotFoundException;
import com.yongyongthon.be.itinerary.data.repository.SlotCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 슬롯 추가, 삭제, 교체, 순서 변경 (지시서 7장 #3, #4, #5, #8).
 * 모두 마지막에 재계산하고 일정 전체를 응답한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItinerarySlotService {

    private static final Set<String> ADDABLE_TYPES =
            Set.of(SlotType.NEARBY_EVENT, SlotType.PROGRAM, SlotType.MEAL, SlotType.CUSTOM);
    private static final int TITLE_MAX = 50;
    private static final int DURATION_MIN = 1;
    private static final int DURATION_MAX = 300;

    private final ItineraryService itineraryService;
    private final EventRepository eventRepository;
    private final SlotCandidateRepository slotCandidateRepository;
    private final SlotFactory slotFactory;

    /** #3 슬롯 추가 */
    @Transactional
    public GlobalResponse<ItineraryResponseDto> addSlot(String itineraryId, SlotAddRequestDto req) {
        validateAdd(req);
        Itinerary it = itineraryService.getItineraryEntity(itineraryId);
        ItinerarySlot after = req.afterSlotId() == null ? null : getSlotEntity(it, req.afterSlotId());

        // 빈 식사 슬롯이 있으면 새로 만들지 않고 그 슬롯을 채운다 (slotId 유지)
        if (SlotType.MEAL.equals(req.type())) {
            ItinerarySlot emptyMeal = it.orderedSlots().stream()
                    .filter(SlotFactory::isEmptyMeal)
                    .findFirst()
                    .orElse(null);
            if (emptyMeal != null) {
                slotFactory.fillUserMeal(emptyMeal, req.title().trim(), req.durationMin(), req.address());
                slotCandidateRepository.deleteBySlot(it.getItineraryId(), emptyMeal.getSlotId());
                log.info("[addSlot] 빈 식사 슬롯 채움: {} {}", itineraryId, emptyMeal.getSlotId());
                return GlobalResponse.ok(itineraryService.toResponse(it));
            }
        }

        ItinerarySlot slot = new ItinerarySlot(null, req.type());
        switch (req.type()) {
            case SlotType.NEARBY_EVENT -> {
                EventInfo event = eventRepository.findById(req.eventId()).orElseThrow(EventNotFoundException::new);
                slotFactory.fillFromEvent(slot, event, SlotType.NEARBY_EVENT);
                slot.setUserAdded(true);
            }
            case SlotType.PROGRAM -> {
                EventInfo event = eventRepository.findById(req.eventId()).orElseThrow(EventNotFoundException::new);
                EventProgram program = eventRepository.findPrograms(event.eventId()).stream()
                        .filter(p -> req.programId().equals(p.programId()))
                        .findFirst()
                        .orElseThrow(InvalidItineraryRequestException::new);
                slotFactory.fillFromProgram(slot, event, program);
                slot.setUserAdded(true);
            }
            case SlotType.MEAL -> slotFactory.fillUserMeal(slot, req.title().trim(), req.durationMin(), req.address());
            default -> slotFactory.fillCustom(slot, req.title().trim(), req.durationMin(), req.costText(), req.address());
        }
        if (req.durationMin() != null) {
            slot.setDurationMin(req.durationMin());
        }

        slot.setSlotId(it.nextSlotId());
        insertAfter(it, slot, after);
        log.info("[addSlot] 슬롯 추가: {} {} {}", itineraryId, slot.getSlotId(), slot.getType());
        return GlobalResponse.ok(itineraryService.toResponse(it));
    }

    /** #4 슬롯 삭제. 기준 행사와 귀가 슬롯은 지울 수 없다 */
    @Transactional
    public GlobalResponse<ItineraryResponseDto> deleteSlot(String itineraryId, String slotId) {
        Itinerary it = itineraryService.getItineraryEntity(itineraryId);
        ItinerarySlot slot = getSlotEntity(it, slotId);
        if (SlotType.EVENT.equals(slot.getType()) || SlotType.RETURN.equals(slot.getType())) {
            throw new SlotNotDeletableException();
        }
        it.removeSlot(slot);
        slotCandidateRepository.deleteBySlot(it.getItineraryId(), slotId);
        log.info("[deleteSlot] 슬롯 삭제: {} {}", itineraryId, slotId);
        return GlobalResponse.ok(itineraryService.toResponse(it));
    }

    /** #5 슬롯 교체. slotId는 유지하고 내용만 바꾼다 */
    @Transactional
    public GlobalResponse<ItineraryResponseDto> replaceSlot(String itineraryId, String slotId,
                                                            SlotReplaceRequestDto req) {
        boolean byCandidate = req != null && req.replaceWithCandidateId() != null;
        boolean byEvent = req != null && req.replaceWithEventId() != null;
        if (byCandidate == byEvent) {
            throw new InvalidItineraryRequestException();
        }
        Itinerary it = itineraryService.getItineraryEntity(itineraryId);
        ItinerarySlot slot = getSlotEntity(it, slotId);
        String type = slot.getType();
        if (SlotType.EVENT.equals(type) || SlotType.RETURN.equals(type)) {
            throw new InvalidItineraryRequestException();
        }
        boolean mealSlot = SlotType.MEAL.equals(type);

        if (byCandidate) {
            String candidateId = req.replaceWithCandidateId();
            if (MealSamples.isMealCandidateId(candidateId)) {
                if (!mealSlot) {
                    throw new InvalidItineraryRequestException();
                }
                MealSamples.MealSample sample = MealSamples.find(candidateId).orElseThrow(SlotNotFoundException::new);
                slotFactory.fillMealSample(slot, sample);
            } else {
                if (mealSlot) {
                    throw new InvalidItineraryRequestException();
                }
                SlotCandidate candidate = slotCandidateRepository
                        .findByItineraryIdAndSlotIdAndCandidateId(itineraryId, slotId, candidateId)
                        .orElseThrow(SlotNotFoundException::new);
                EventInfo event = eventRepository.findById(candidate.getEventId())
                        .orElseThrow(EventNotFoundException::new);
                slotFactory.fillFromEvent(slot, event, SlotType.NEARBY_EVENT);
            }
        } else {
            if (mealSlot) {
                throw new InvalidItineraryRequestException();
            }
            EventInfo event = eventRepository.findById(req.replaceWithEventId())
                    .orElseThrow(EventNotFoundException::new);
            slotFactory.fillFromEvent(slot, event, SlotType.NEARBY_EVENT);
        }

        slotCandidateRepository.deleteBySlot(itineraryId, slotId);
        log.info("[replaceSlot] 슬롯 교체: {} {}", itineraryId, slotId);
        return GlobalResponse.ok(itineraryService.toResponse(it));
    }

    /** #8 순서 변경. return 슬롯은 빼고 보내며 항상 마지막에 둔다 */
    @Transactional
    public GlobalResponse<ItineraryResponseDto> reorderSlots(String itineraryId, SlotOrderRequestDto req) {
        Itinerary it = itineraryService.getItineraryEntity(itineraryId);
        if (req == null || req.slotIds() == null) {
            throw new InvalidItineraryRequestException();
        }
        List<ItinerarySlot> movable = it.orderedSlots().stream()
                .filter(s -> !SlotType.RETURN.equals(s.getType()))
                .toList();
        List<String> ids = req.slotIds();
        Set<String> unique = new HashSet<>(ids);
        Set<String> existing = new HashSet<>();
        movable.forEach(s -> existing.add(s.getSlotId()));
        if (ids.size() != movable.size() || unique.size() != ids.size() || !unique.equals(existing)) {
            throw new InvalidItineraryRequestException();
        }
        for (int i = 0; i < ids.size(); i++) {
            getSlotEntity(it, ids.get(i)).setSlotOrder(i + 1);
        }
        log.info("[reorderSlots] 순서 변경: {}", itineraryId);
        return GlobalResponse.ok(itineraryService.toResponse(it));
    }

    public static ItinerarySlot getSlotEntity(Itinerary it, String slotId) {
        return it.getSlots().stream()
                .filter(s -> s.getSlotId().equals(slotId))
                .findFirst()
                .orElseThrow(SlotNotFoundException::new);
    }

    /** after 뒤에 넣는다. after가 없거나 귀가 슬롯이면 귀가 직전 */
    private static void insertAfter(Itinerary it, ItinerarySlot slot, ItinerarySlot after) {
        List<ItinerarySlot> ordered = it.orderedSlots().stream()
                .filter(s -> !SlotType.RETURN.equals(s.getType()))
                .toList();
        int position = ordered.size();
        if (after != null && !SlotType.RETURN.equals(after.getType())) {
            position = ordered.indexOf(after) + 1;
        }
        int order = 1;
        for (int i = 0; i < ordered.size(); i++) {
            if (i == position) {
                order++;
            }
            ordered.get(i).setSlotOrder(order++);
        }
        slot.setSlotOrder(position + 1);
        it.addSlot(slot);
    }

    private static void validateAdd(SlotAddRequestDto req) {
        if (req == null || req.type() == null || !ADDABLE_TYPES.contains(req.type())) {
            throw new InvalidItineraryRequestException();
        }
        if (req.durationMin() != null && (req.durationMin() < DURATION_MIN || req.durationMin() > DURATION_MAX)) {
            throw new InvalidItineraryRequestException();
        }
        switch (req.type()) {
            case SlotType.NEARBY_EVENT -> requireText(req.eventId());
            case SlotType.PROGRAM -> {
                requireText(req.eventId());
                requireText(req.programId());
            }
            default -> {
                requireText(req.title());
                if (req.title().trim().length() > TITLE_MAX) {
                    throw new InvalidItineraryRequestException();
                }
            }
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidItineraryRequestException();
        }
    }
}
