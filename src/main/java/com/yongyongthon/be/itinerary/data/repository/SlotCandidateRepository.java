package com.yongyongthon.be.itinerary.data.repository;

import com.yongyongthon.be.itinerary.data.entity.SlotCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SlotCandidateRepository extends JpaRepository<SlotCandidate, Long> {

    Optional<SlotCandidate> findByItineraryIdAndSlotIdAndCandidateId(String itineraryId, String slotId,
                                                                    String candidateId);

    @Modifying
    @Query("delete from SlotCandidate c where c.itineraryId = :itineraryId and c.slotId = :slotId")
    void deleteBySlot(@Param("itineraryId") String itineraryId, @Param("slotId") String slotId);

    @Modifying
    @Query("delete from SlotCandidate c where c.itineraryId = :itineraryId")
    void deleteByItinerary(@Param("itineraryId") String itineraryId);
}
