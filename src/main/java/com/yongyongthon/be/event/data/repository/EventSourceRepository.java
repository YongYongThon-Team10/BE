package com.yongyongthon.be.event.data.repository;

import com.yongyongthon.be.event.data.entity.EventSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSourceRepository extends JpaRepository<EventSource, Long> {
    List<EventSource> findByEventIdOrderBySortOrder(String eventId);
}
