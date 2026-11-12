package {{PACKAGE}}.common.security.exception;

import {{PACKAGE}}.common.response.ErrorCode;
import {{PACKAGE}}.common.response.GlobalException;

public class AuthenticationInfoException extends GlobalException {
    public AuthenticationInfoException() {
        super(ErrorCode.VERIFICATION_INVALID);
    }
}
