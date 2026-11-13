package com.yongyongthon.be.itinerary.data.repository;

import com.yongyongthon.be.itinerary.data.entity.ItineraryShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItineraryShareRepository extends JpaRepository<ItineraryShare, String> {

    Optional<ItineraryShare> findByItineraryId(String itineraryId);
}
