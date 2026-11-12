package com.yongyongthon.be.user.data.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class PasswordInvalidException extends GlobalException {
    public PasswordInvalidException() {
        super(ErrorCode.PASSWORD_INVALID);
    }
}