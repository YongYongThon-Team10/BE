package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.response.RequestValueInvalidException;
import com.yongyongthon.be.common.util.Ids;
import com.yongyongthon.be.course.data.repository.CourseRepository;
import com.yongyongthon.be.event.data.dto.EventInfo;
import com.yongyongthon.be.event.data.exception.EventNotFoundException;
import com.yongyongthon.be.event.service.EventCatalog;
import com.yongyongthon.be.itinerary.data.dto.ItineraryCreateRequestDto;
import com.yongyongthon.be.itinerary.data.dto.ItineraryRequestDto;
import com.yongyongthon.be.itinerary.data.dto.ItineraryResponseDto;
import com.yongyongthon.be.itinerary.data.dto.WarningResponseDto;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.exception.ItineraryNotFoundException;
import com.yongyongthon.be.itinerary.data.repository.ItineraryRepository;
import com.yongyongthon.be.itinerary.data.repository.SlotCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 일정 생성, 조회, 재추천 (지시서 7장 #1, #2, #9).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ItineraryRepository itineraryRepository;
    private final CourseRepository courseRepository;
    private final EventCatalog eventCatalog;
    private final ItineraryComposer composer;
    private final Recalculator recalculator;
    private final SlotCandidateRepository slotCandidateRepository;
    private final ItineraryRequestValidator itineraryRequestValidator;

    /**
     * #1 생성. 요청 본문은 컨트롤러에서 @Valid로 검사하고(실패 시 E4001),
     * 시간대 역전과 기준 행사 존재는 ItineraryRequestValidator가 검사한다 (E4001, E4041).
     */
    @Transactional
    public GlobalResponse<ItineraryResponseDto> createItinerary(ItineraryCreateRequestDto request) {
        EventInfo base = EventInfo.from(itineraryRequestValidator.validate(request));
        checkDateInEventPeriod(base, request.getDate());

        Itinerary it = new Itinerary(Ids.random("itn_", 8));
        it.setBaseEventId(base.eventId());
        it.setTitle(base.name() + " 중심 하루");
        it.setCoverImageUrl(base.thumbnailUrl());
        it.setBudget("any");
        applyConditions(it, toConditions(request));

        composer.compose(it, base);
        List<WarningResponseDto> warnings = recalculator.recalculate(it);
        itineraryRepository.save(it);
        log.info("[createItinerary] 일정 생성: {}", it.getItineraryId());
        return GlobalResponse.ok(ItineraryResponseDto.of(it, warnings, false));
    }

    @Transactional(readOnly = true)
    public GlobalResponse<ItineraryResponseDto> getItinerary(String itineraryId) {
        return GlobalResponse.ok(toResponse(getItineraryEntity(itineraryId)));
    }

    /**
     * #9 재추천. 보낸 항목만 바꾸고 나머지는 기존 값을 유지한 채 6-1 절차를 처음부터 다시 실행한다.
     * itineraryId, title, coverImageUrl과 저장된 코스 연결은 유지하고 슬롯은 새 slotId로 모두 새로 만든다.
     * 사용자가 직접 추가한 슬롯은 버린다. 실패하면 트랜잭션이 롤백되어 기존 일정이 그대로 남는다.
     */
    @Transactional
    public GlobalResponse<ItineraryResponseDto> regenerateItinerary(String itineraryId, ItineraryRequestDto request) {
        Itinerary it = getItineraryEntity(itineraryId);
        ItineraryRequestDto req = request != null ? request
                : new ItineraryRequestDto(null, null, null, null, null, null, null, null, null);
        if (req.baseEventId() != null && !req.baseEventId().equals(it.getBaseEventId())) {
            throw new RequestValueInvalidException();
        }
        ItineraryConditionValidator.validatePresent(req);
        EventInfo base = eventCatalog.findById(it.getBaseEventId())
                .orElseThrow(EventNotFoundException::new);
        LocalDate date = req.date() != null ? ItineraryConditionValidator.parseDate(req.date()) : it.getDate();
        checkDateInEventPeriod(base, date);

        applyConditions(it, req);
        composer.compose(it, base);
        slotCandidateRepository.deleteByItinerary(itineraryId);
        log.info("[regenerateItinerary] 일정 재추천: {}", itineraryId);
        return GlobalResponse.ok(toResponse(it));
    }

    /** 수정 API 공통: 재계산 후 전체 응답 */
    public ItineraryResponseDto toResponse(Itinerary it) {
        List<WarningResponseDto> warnings = recalculator.recalculate(it);
        boolean saved = courseRepository.existsByItineraryId(it.getItineraryId());
        return ItineraryResponseDto.of(it, warnings, saved);
    }

    public Itinerary getItineraryEntity(String itineraryId) {
        return itineraryRepository.findById(itineraryId)
                .orElseThrow(ItineraryNotFoundException::new);
    }

    /** 생성 요청을 재추천과 같은 조건 모양으로 바꾼다. 시각은 HH:mm 문자열로 저장한다 */
    static ItineraryRequestDto toConditions(ItineraryCreateRequestDto r) {
        ItineraryCreateRequestDto.Origin o = r.getOrigin();
        return new ItineraryRequestDto(
                r.getBaseEventId(),
                r.getDate().toString(),
                new ItineraryRequestDto.Origin(o.getType(), o.getLat(), o.getLng(), o.getAddress()),
                r.getTransport(),
                new ItineraryRequestDto.TimeWindow(
                        r.getTimeWindow().getStart().format(HH_MM), r.getTimeWindow().getEnd().format(HH_MM)),
                r.getCompanion(),
                r.getKidsAge(),
                r.getBudget(),
                r.getWants() == null ? null : r.getWants().stream().filter(Objects::nonNull).toList());
    }

    /** 보낸 항목만 덮어쓴다 (생성은 필수값이 모두 있음) */
    static void applyConditions(Itinerary it, ItineraryRequestDto req) {
        if (req.date() != null) {
            it.setDate(ItineraryConditionValidator.parseDate(req.date()));
        }
        if (req.origin() != null) {
            it.setOriginType(req.origin().type());
            it.setOriginLat(req.origin().lat());
            it.setOriginLng(req.origin().lng());
            it.setOriginAddress(req.origin().address());
        }
        if (req.transport() != null) {
            it.setTransport(req.transport());
        }
        if (req.timeWindow() != null) {
            it.setWindowStart(req.timeWindow().start());
            it.setWindowEnd(req.timeWindow().end());
        }
        if (req.companion() != null) {
            it.setCompanion(req.companion());
        }
        if (req.kidsAge() != null) {
            it.setKidsAge(req.kidsAge());
        }
        if (req.budget() != null) {
            it.setBudget(req.budget());
        }
        if (req.wants() != null) {
            it.setWantsList(req.wants());
        }
    }

    /** 지시서 11장 #2: 일정 날짜가 기준 행사 기간 밖이면 거절 (기간이 null이면 허용) */
    static void checkDateInEventPeriod(EventInfo base, LocalDate date) {
        if ((base.startDate() != null && date.isBefore(base.startDate()))
                || (base.endDate() != null && date.isAfter(base.endDate()))) {
            throw new RequestValueInvalidException();
        }
    }
}
