package com.yongyongthon.be.common.ai.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class AiTimeoutException extends GlobalException {
    public AiTimeoutException() {
        super(ErrorCode.AI_TIMEOUT);
    }
}
