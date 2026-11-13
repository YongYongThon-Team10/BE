package com.yongyongthon.be.itinerary.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class SlotNotDeletableException extends GlobalException {
    public SlotNotDeletableException() {
        super(ErrorCode.SLOT_NOT_DELETABLE);
    }
}
