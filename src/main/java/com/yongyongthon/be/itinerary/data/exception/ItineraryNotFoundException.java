package com.yongyongthon.be.itinerary.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class ItineraryNotFoundException extends GlobalException {
    public ItineraryNotFoundException() {
        super(ErrorCode.ITINERARY_NOT_FOUND);
    }
}
