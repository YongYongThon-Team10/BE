package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.event.data.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, String> {
}
