package com.yongyongthon.be.user.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class UserNotFoundException extends GlobalException {
    public UserNotFoundException() {
        super(ErrorCode.USER_DATA_NOT_FOUND);
    }
}