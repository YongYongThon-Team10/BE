package com.yongyongthon.be.course.data.entity;

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

import java.time.LocalDateTime;

/**
 * 내 코스 (지시서 5장 courses). 사용자 컬럼 없음, 같은 일정은 한 번만 저장.
 * 응답의 courseId는 "crs_" + id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(name = "itinerary_id", nullable = false, unique = true)
    private String itineraryId;

    private String title;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    public String courseId() {
        return "crs_" + id;
    }
}
