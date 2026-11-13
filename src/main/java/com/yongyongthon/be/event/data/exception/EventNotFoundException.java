package com.yongyongthon.be.event.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class EventNotFoundException extends GlobalException {
    public EventNotFoundException() {
        super(ErrorCode.EVENT_NOT_FOUND);
    }
}
