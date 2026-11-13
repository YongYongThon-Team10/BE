package com.yongyongthon.be.common.ai.exception;

import com.yongyongthon.be.common.response.ErrorCode;
import com.yongyongthon.be.common.response.GlobalException;

public class AiServerException extends GlobalException {
    public AiServerException() {
        super(ErrorCode.AI_SERVER_ERROR);
    }
}
