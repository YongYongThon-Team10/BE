package com.yongyongthon.be.common.response;

/**
 * 애너테이션 검증으로 잡을 수 없는 요청 값 오류(예: dateFrom이 dateTo보다 늦음)를 서비스에서 던질 때 쓴다.
 */
public class RequestValueInvalidException extends GlobalException {
    public RequestValueInvalidException() {
        super(ErrorCode.REQUEST_VALUE_INVALID);
    }
}
