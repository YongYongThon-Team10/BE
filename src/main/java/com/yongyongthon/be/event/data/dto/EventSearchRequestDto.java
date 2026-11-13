package com.yongyongthon.be.event.data.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * E1 GET /api/events 쿼리 파라미터. 검증 실패는 PublicApiExceptionHandler가 E4001로 응답한다.
 * 복수 값(district, dong)은 콤마로 구분해 보낸다.
 */
@Data
@NoArgsConstructor
public class EventSearchRequestDto {

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    // 생략하면 dateFrom과 같다
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private List<@Pattern(regexp = "처인구|기흥구|수지구") String> district;

    private List<String> dong;

    // 근거 데이터가 없어 값 검증만 하고 필터에는 쓰지 않는다 (계획 문서 결정 3)
    @Pattern(regexp = "alone|couple|friends|family|kids")
    private String companion;

    @Pattern(regexp = "any|free")
    private String budget;

    // companion과 같은 이유로 필터에는 쓰지 않는다
    @Pattern(regexp = "indoor|outdoor|any")
    private String indoorOutdoor;

    @Pattern(regexp = "date|distance")
    private String sort = "date";

    @DecimalMin("-90") @DecimalMax("90")
    private Double lat;

    @DecimalMin("-180") @DecimalMax("180")
    private Double lng;

    @Pattern(regexp = "list|map")
    private String view = "list";

    @Min(0)
    private int page = 0;

    @Min(1) @Max(100)
    private int size = 20;
}
