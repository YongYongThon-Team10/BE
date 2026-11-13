package com.yongyongthon.be.itinerary.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class InvalidItineraryRequestException extends GlobalException {
    public InvalidItineraryRequestException() {
        super(ErrorCode.INVALID_REQUEST);
    }
}
