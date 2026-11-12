package {{PACKAGE}}.user.data.exception;

import {{PACKAGE}}.common.response.ErrorCode;
import {{PACKAGE}}.common.response.GlobalException;

public class PasswordInvalidException extends GlobalException {
    public PasswordInvalidException() {
        super(ErrorCode.PASSWORD_INVALID);
    }
}