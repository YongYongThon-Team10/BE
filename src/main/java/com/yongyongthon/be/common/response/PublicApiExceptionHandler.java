package com.yongyongthon.be.common.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 명세서01 기준 API(행사, 검색, 일정, 장소)의 요청 값 오류를 E4001로 응답한다.
 * 인증 API는 이 핸들러 대상이 아니므로 기존 GlobalExceptionHandler의 C002를 그대로 쓴다.
 * 새 패키지가 같은 규칙을 따라야 하면 basePackages에 추가한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.yongyongthon.be.event",
        "com.yongyongthon.be.search",
        "com.yongyongthon.be.itinerary",
        "com.yongyongthon.be.place"
})
public class PublicApiExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<GlobalResponse<?>> handleRequestValueException(Exception e) {
        log.warn("[handleRequestValueException] 요청 값 검증 실패: {}", e.getMessage());

        return ResponseEntity
                .status(ErrorCode.REQUEST_VALUE_INVALID.getHttpStatus())
                .body(GlobalResponse.error(ErrorCode.REQUEST_VALUE_INVALID));
    }
}
