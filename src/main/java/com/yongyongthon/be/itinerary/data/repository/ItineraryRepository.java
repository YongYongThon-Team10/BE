package com.yongyongthon.be.itinerary.data.repository;

import com.yongyongthon.be.itinerary.data.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository extends JpaRepository<Itinerary, String> {
}
