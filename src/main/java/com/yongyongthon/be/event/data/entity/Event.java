package com.yongyongthon.be.event.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 행사. 데이터는 scripts/seed_events.py가 팀 행사 DB(또는 엑셀)에서 변환해 넣는다 (앱은 읽기만 한다).
 * 엑셀에 없는 명세 필드(summary, themeLabel, imageUrl, indoorOutdoor, tags, notice)는 컬럼을 두지 않고
 * 응답에서 null 또는 빈 배열로 내보낸다. 데이터가 생기면 컬럼을 추가한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    // 엑셀 행사ID (예: YI-0001)
    @Id
    @Column(name = "event_id", length = 20)
    private String id;

    @Column(nullable = false, length = 300)
    private String name;

    // 공연 또는 행사
    @Column(nullable = false, length = 10)
    private String category;

    // 엑셀 분류 원문 (도서관 프로그램, 전시, 축제 등)
    @Column(length = 50)
    private String subCategory;

    private LocalDate startDate;

    private LocalDate endDate;

    // 확정, 예정, 연간 계획, 미확인
    @Column(nullable = false, length = 10)
    private String dateStatus;

    // 시간 원문 (예: 10:00 ~ 12:00)
    @Column(length = 100)
    private String time;

    @Column(length = 200)
    private String placeName;

    @Column(length = 200)
    private String address;

    // 처인구, 기흥구, 수지구
    @Column(length = 10)
    private String district;

    private Double lat;

    private Double lng;

    // place 또는 area
    @Column(length = 10)
    private String coordPrecision;

    // 요금 원문
    @Column(length = 200)
    private String fee;

    // 무료 확인이면 true, 유료 확인이면 false, 모르면 null
    private Boolean isFree;

    @Column(length = 300)
    private String target;

    // apply(신청 필요) 또는 null. 엑셀에 현장 참여 표시가 없어 walkin은 쓰지 않는다
    @Column(length = 10)
    private String applyType;

    @Column(length = 1000)
    private String applyUrl;

    private LocalDateTime receiptStart;

    private LocalDateTime receiptEnd;

    // 접수 상태 원문 (신청하기, 마감(대기신청))
    @Column(length = 50)
    private String receiptStatus;

    // 대표 출처 링크
    @Column(nullable = false, length = 1000)
    private String sourceUrl;
}
