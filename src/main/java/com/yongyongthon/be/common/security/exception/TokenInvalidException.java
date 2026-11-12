package com.yongyongthon.be.common.security.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class TokenInvalidException extends GlobalException {
    public TokenInvalidException() {
        super(ErrorCode.TOKEN_INVALID);
    }
}
