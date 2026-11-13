package com.yongyongthon.be.common.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<GlobalResponse<?>> handleGlobalException(GlobalException e) {
        HttpStatus status = HttpStatus.valueOf(e.getErrorCode().getHttpStatus());

        return ResponseEntity
                .status(status)
                .body(GlobalResponse.error(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_PARAMETER.getMessage());
        log.warn("[handleValidationException] 요청 값 검증 실패: {}", detail);

        return ResponseEntity
                .status(ErrorCode.INVALID_PARAMETER.getHttpStatus())
                .body(GlobalResponse.error(ErrorCode.INVALID_PARAMETER));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<GlobalResponse<?>> handleBadRequest(Exception e) {
        log.warn("[handleBadRequest] 요청 형식 오류: {}", e.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(GlobalResponse.error(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleException(Exception e) {
        log.error("[handleException] 처리되지 않은 예외 발생", e);

        return ResponseEntity
                .status(ErrorCode.UNKNOWN_ERROR.getHttpStatus())
                .body(GlobalResponse.error(ErrorCode.UNKNOWN_ERROR));
    }
}
