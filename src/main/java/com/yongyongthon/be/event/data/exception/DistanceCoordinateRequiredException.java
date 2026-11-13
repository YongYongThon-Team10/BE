package com.yongyongthon.be.event.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class DistanceCoordinateRequiredException extends GlobalException {
    public DistanceCoordinateRequiredException() {
        super(ErrorCode.DISTANCE_COORDINATE_REQUIRED);
    }
}
