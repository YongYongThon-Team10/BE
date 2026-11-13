package com.yongyongthon.be.event.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 행사의 출처 링크. 엑셀의 대표출처URL과 추가출처URL을 한 줄씩 나눈 것이다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_sources")
public class EventSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    // official, blog, etc
    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false, length = 1000)
    private String url;

    // 엑셀에 적힌 순서 (0이 대표 출처)
    @Column(nullable = false)
    private Integer sortOrder;
}
