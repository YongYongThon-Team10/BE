package {{PACKAGE}}.common.security.exception;

import {{PACKAGE}}.common.response.ErrorCode;
import {{PACKAGE}}.common.response.GlobalException;

public class TokenInvalidException extends GlobalException {
    public TokenInvalidException() {
        super(ErrorCode.TOKEN_INVALID);
    }
}
