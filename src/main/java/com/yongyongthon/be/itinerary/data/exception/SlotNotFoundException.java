package com.yongyongthon.be.itinerary.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class SlotNotFoundException extends GlobalException {
    public SlotNotFoundException() {
        super(ErrorCode.SLOT_NOT_FOUND);
    }
}
