package com.yongyongthon.be.itinerary.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일정 항목 (지시서 5장 itinerary_slots). slot_id는 일정 안에서만 유일하다.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "itinerary_slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"itinerary_id", "slot_id"}))
public class ItinerarySlot {

    public static final String SUGGESTED = "suggested";
    public static final String OFFICIAL = "official";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @Column(name = "slot_id", nullable = false, length = 20)
    private String slotId;

    @Column(name = "slot_order")
    private int slotOrder;

    @Column(name = "slot_type", nullable = false, length = 20)
    private String type;

    private String title;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "program_id")
    private String programId;

    @Column(name = "start_time", length = 5)
    private String startTime;

    @Column(name = "end_time", length = 5)
    private String endTime;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "cost_amount")
    private Integer costAmount;

    @Column(name = "cost_text", length = 500)
    private String costText;

    @Column(length = 2000)
    private String description;

    @Column(name = "time_basis", length = 20)
    private String timeBasis = SUGGESTED;

    @Column(name = "is_user_added")
    private boolean userAdded;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "place_address")
    private String placeAddress;

    @Column(name = "place_lat")
    private Double placeLat;

    @Column(name = "place_lng")
    private Double placeLng;

    @Column(name = "move_mode", length = 20)
    private String moveMode;

    @Column(name = "move_distance_km")
    private Double moveDistanceKm;

    @Column(name = "move_estimated_min")
    private Integer moveEstimatedMin;

    @Column(name = "move_map_link", length = 1000)
    private String moveMapLink;

    /** slotId는 일정을 구성한 뒤 순서대로 붙이므로 처음엔 null일 수 있다 */
    public ItinerarySlot(String slotId, String type) {
        this.slotId = slotId;
        this.type = type;
    }

    public boolean hasPlace() {
        return placeName != null || placeAddress != null || placeLat != null || placeLng != null;
    }

    public boolean hasCoords() {
        return placeLat != null && placeLng != null;
    }

    public void setPlace(String name, String address, Double lat, Double lng) {
        this.placeName = name;
        this.placeAddress = address;
        this.placeLat = lat;
        this.placeLng = lng;
    }

    public void clearPlace() {
        setPlace(null, null, null, null);
    }

    public void setMove(String mode, double distanceKm, int estimatedMin, String mapLink) {
        this.moveMode = mode;
        this.moveDistanceKm = distanceKm;
        this.moveEstimatedMin = estimatedMin;
        this.moveMapLink = mapLink;
    }

    public void clearMove() {
        this.moveMode = null;
        this.moveDistanceKm = null;
        this.moveEstimatedMin = null;
        this.moveMapLink = null;
    }

    public boolean hasMove() {
        return moveMode != null;
    }

    /** 내용만 비운다 (slotId, type, order, isUserAdded는 유지) */
    public void clearContent() {
        title = null;
        eventId = null;
        programId = null;
        durationMin = null;
        costAmount = null;
        costText = null;
        description = null;
        timeBasis = SUGGESTED;
        clearPlace();
        clearMove();
    }
}
