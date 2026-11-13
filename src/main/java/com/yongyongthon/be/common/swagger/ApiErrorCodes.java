package com.yongyongthon.be.common.swagger;

import com.yongyongthon.be.common.response.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 API가 돌려줄 수 있는 오류 코드. Swagger 문서에 상태 코드별 오류 응답 예시로 표시된다.
 * C500(UNKNOWN_ERROR)은 적지 않아도 항상 추가된다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {
    ErrorCode[] value();
}
