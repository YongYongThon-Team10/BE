package com.yongyongthon.be.itinerary.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class SharedItineraryNotFoundException extends GlobalException {
    public SharedItineraryNotFoundException() {
        super(ErrorCode.SHARED_ITINERARY_NOT_FOUND);
    }
}
