package com.yongyongthon.be.itinerary.data.entity;

import com.yongyongthon.be.itinerary.data.enums.SlotType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 일정 (지시서 5장 itineraries).
 * 재추천이 "보내지 않은 항목은 기존 값 유지"라서 입력 조건을 모두 저장한다.
 * 슬롯과 양방향 연관이라 @Data 대신 @Getter, @Setter를 쓴다 (toString, hashCode 순환 방지).
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @Column(name = "itinerary_id", length = 40)
    private String itineraryId;

    @Column(name = "base_event_id", nullable = false)
    private String baseEventId;

    private String title;

    @Column(name = "itinerary_date", nullable = false)
    private LocalDate date;

    @Column(length = 20)
    private String transport;

    @Column(name = "window_start", length = 5)
    private String windowStart;

    @Column(name = "window_end", length = 5)
    private String windowEnd;

    @Column(name = "origin_type", length = 20)
    private String originType;

    @Column(name = "origin_lat")
    private Double originLat;

    @Column(name = "origin_lng")
    private Double originLng;

    @Column(name = "origin_address")
    private String originAddress;

    @Column(length = 20)
    private String companion;

    @Column(name = "kids_age")
    private Integer kidsAge;

    @Column(length = 20)
    private String budget;

    /** 콤마로 이은 문자열 */
    private String wants;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    /** slotId 번호를 이어 붙이기 위한 값. 재생성해도 이전 slotId와 겹치지 않는다 */
    @Column(name = "slot_seq")
    private int slotSeq;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("slotOrder ASC")
    private List<ItinerarySlot> slots = new ArrayList<>();

    public Itinerary(String itineraryId) {
        this.itineraryId = itineraryId;
    }

    public String nextSlotId() {
        slotSeq++;
        return "slt_" + slotSeq;
    }

    /** order 순으로 정렬하고 return 슬롯을 마지막으로 보낸 새 목록 */
    public List<ItinerarySlot> orderedSlots() {
        List<ItinerarySlot> list = new ArrayList<>(slots);
        list.sort(Comparator
                .comparing((ItinerarySlot s) -> SlotType.RETURN.equals(s.getType()) ? 1 : 0)
                .thenComparingInt(ItinerarySlot::getSlotOrder));
        return list;
    }

    public void addSlot(ItinerarySlot slot) {
        slot.setItinerary(this);
        slots.add(slot);
    }

    public void removeSlot(ItinerarySlot slot) {
        slots.removeIf(s -> s == slot);
    }

    public void clearSlots() {
        slots.clear();
    }

    public List<String> getWantsList() {
        if (wants == null || wants.isBlank()) {
            return List.of();
        }
        return Arrays.stream(wants.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void setWantsList(List<String> list) {
        this.wants = list == null || list.isEmpty() ? null : String.join(",", list);
    }
}
