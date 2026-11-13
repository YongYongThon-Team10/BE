package com.yongyongthon.be.place.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class PlaceSearchFailedException extends GlobalException {
    public PlaceSearchFailedException() {
        super(ErrorCode.PLACE_SEARCH_FAILED);
    }
}
