package com.yongyongthon.be.common.security.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class AuthenticationInfoException extends GlobalException {
    public AuthenticationInfoException() {
        super(ErrorCode.VERIFICATION_INVALID);
    }
}
