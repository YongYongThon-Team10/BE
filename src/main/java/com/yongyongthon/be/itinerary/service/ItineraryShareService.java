package com.yongyongthon.be.itinerary.service;

import com.yongyongthon.be.common.config.AppProperties;
import com.yongyongthon.be.common.response.GlobalResponse;
import com.yongyongthon.be.common.util.Ids;
import com.yongyongthon.be.itinerary.data.dto.ItineraryResponseDto;
import com.yongyongthon.be.itinerary.data.dto.ShareResponseDto;
import com.yongyongthon.be.itinerary.data.dto.SharedItineraryResponseDto;
import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import com.yongyongthon.be.itinerary.data.entity.ItineraryShare;
import com.yongyongthon.be.itinerary.data.entity.ItinerarySlot;
import com.yongyongthon.be.itinerary.data.enums.SlotType;
import com.yongyongthon.be.itinerary.data.exception.SharedItineraryNotFoundException;
import com.yongyongthon.be.itinerary.data.repository.ItineraryRepository;
import com.yongyongthon.be.itinerary.data.repository.ItineraryShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공유 링크 만들기와 공유 일정 미리보기 (지시서 7장 #10, #11).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryShareService {

    private final ItineraryService itineraryService;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryShareRepository itineraryShareRepository;
    private final Recalculator recalculator;
    private final AppProperties props;

    /** #10 같은 일정은 항상 같은 shareId */
    @Transactional
    public GlobalResponse<ShareResponseDto> shareItinerary(String itineraryId) {
        Itinerary it = itineraryService.getItineraryEntity(itineraryId);
        ItineraryShare share = itineraryShareRepository.findByItineraryId(itineraryId)
                .orElseGet(() -> {
                    ItineraryShare created = itineraryShareRepository.save(ItineraryShare.builder()
                            .shareId(Ids.random("shr_", 6))
                            .itineraryId(itineraryId)
                            .build());
                    log.info("[shareItinerary] 공유 링크 생성: {} {}", itineraryId, created.getShareId());
                    return created;
                });
        return GlobalResponse.ok(new ShareResponseDto(share.getShareId(), shareUrl(share.getShareId()), it.getTitle()));
    }

    /** #11 읽기 전용 미리보기 */
    @Transactional(readOnly = true)
    public GlobalResponse<SharedItineraryResponseDto> getSharedItinerary(String shareId) {
        ItineraryShare share = itineraryShareRepository.findById(shareId)
                .orElseThrow(SharedItineraryNotFoundException::new);
        Itinerary it = itineraryRepository.findById(share.getItineraryId())
                .orElseThrow(SharedItineraryNotFoundException::new);
        recalculator.recalculate(it);
        List<ItinerarySlot> ordered = it.orderedSlots();

        List<SharedItineraryResponseDto.SharedSlot> slots = ordered.stream()
                .map(s -> new SharedItineraryResponseDto.SharedSlot(
                        s.getSlotOrder(),
                        s.getType(),
                        s.getTitle(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getDurationMin(),
                        s.getCostText(),
                        sharedPlace(s)))
                .toList();

        return GlobalResponse.ok(new SharedItineraryResponseDto(
                share.getShareId(),
                it.getTitle(),
                it.getDate().toString(),
                it.getCompanion(),
                ordered.size(),
                ItineraryResponseDto.totalCost(ordered),
                ItineraryResponseDto.costUnknownCount(ordered),
                it.getCoverImageUrl(),
                slots));
    }

    /** 귀가 슬롯의 place는 출발지라서 공유하지 않는다 */
    private static SharedItineraryResponseDto.SharedPlace sharedPlace(ItinerarySlot s) {
        if (SlotType.RETURN.equals(s.getType()) || !s.hasPlace()) {
            return null;
        }
        return new SharedItineraryResponseDto.SharedPlace(s.getPlaceName(), s.getPlaceLat(), s.getPlaceLng());
    }

    private String shareUrl(String shareId) {
        String base = props.getShareBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + shareId;
    }
}
