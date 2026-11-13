package com.yongyongthon.be.common.response;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("C001", "잘못된 요청입니다.", 400),
    INVALID_PARAMETER("C002", "유효하지 않은 파라미터입니다.", 400),
    VERIFICATION_INVALID("C4011", "인증 정보가 유효하지 않습니다.", 401),
    PASSWORD_INVALID("C4012", "비밀번호가 일치하지 않습니다.", 401),
    TOKEN_INVALID("C4013", "유효하지 않은 토큰입니다.", 401),
    ACCESS_DENIED("C403", "승인되지 않은 사용자입니다.", 403),
    USER_DATA_NOT_FOUND("C4041", "사용자를 찾을 수 없습니다.", 404),
    DATA_NOT_FOUND("C404", "정보를 불러올 수 없습니다.", 404),
    EMAIL_DUPLICATION("C4091", "이미 존재하는 이메일입니다.", 409),
    UNKNOWN_ERROR("C500", "오류가 발생하였습니다.", 500),

    // 행사, 검색, 일정 API (명세서01의 E 코드)
    REQUEST_VALUE_INVALID("E4001", "요청 값이 올바르지 않습니다.", 400),
    DISTANCE_COORDINATE_REQUIRED("E4002", "거리순 정렬에는 좌표가 필요합니다.", 400),
    SLOT_NOT_DELETABLE("E4003", "삭제할 수 없는 항목입니다.", 400),
    EVENT_NOT_FOUND("E4041", "행사를 찾을 수 없습니다.", 404),
    ITINERARY_NOT_FOUND("E4042", "일정을 찾을 수 없습니다.", 404),
    SLOT_NOT_FOUND("E4043", "일정 항목을 찾을 수 없습니다.", 404),
    SHARED_ITINERARY_NOT_FOUND("E4044", "공유된 일정을 찾을 수 없습니다.", 404),
    AI_SERVER_ERROR("E5021", "AI 서버 호출에 실패했습니다.", 502),
    AI_TIMEOUT("E5041", "AI 응답 시간이 초과되었습니다.", 504),
    // 명세서01에 없는 코드 (E14 오류 명세가 없어 신형이 정함, 팀 확인 필요)
    PLACE_SEARCH_FAILED("E5022", "장소 검색에 실패했습니다.", 502);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}