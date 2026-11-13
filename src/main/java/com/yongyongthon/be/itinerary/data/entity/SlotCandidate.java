package com.yongyongthon.be.itinerary.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 가장 최근 alternatives 응답의 후보 (지시서 5장 slot_candidates).
 * PUT의 replaceWithCandidateId로 찾을 때 쓴다. payload는 후보 JSON 그대로.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slot_candidates")
public class SlotCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_id", nullable = false)
    private String itineraryId;

    @Column(name = "slot_id", nullable = false)
    private String slotId;

    @Column(name = "candidate_id", nullable = false)
    private String candidateId;

    @Column(name = "event_id")
    private String eventId;

    @Column(length = 4000)
    private String payload;
}
