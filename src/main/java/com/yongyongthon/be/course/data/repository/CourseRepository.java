package com.yongyongthon.be.course.data.repository;

import com.yongyongthon.be.course.data.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByItineraryId(String itineraryId);

    Optional<Course> findByItineraryId(String itineraryId);
}
