package com.yongyongthon.be.itinerary.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 공유 링크 (지시서 5장 itinerary_shares). 같은 일정은 같은 shareId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "itinerary_shares")
public class ItineraryShare {

    @Id
    @Column(name = "share_id", length = 40)
    private String shareId;

    @Column(name = "itinerary_id", nullable = false, unique = true)
    private String itineraryId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
