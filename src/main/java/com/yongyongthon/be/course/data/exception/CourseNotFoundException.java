package com.yongyongthon.be.course.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

/**
 * 지시서 3-4: 없거나 이미 삭제된 코스도 새 코드를 만들지 않고 "일정을 찾을 수 없습니다"(C4043)를 쓴다.
 */
public class CourseNotFoundException extends GlobalException {
    public CourseNotFoundException() {
        super(ErrorCode.ITINERARY_NOT_FOUND);
    }
}
