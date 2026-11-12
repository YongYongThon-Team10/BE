package {{PACKAGE}}.user.data.exception;

import {{PACKAGE}}.common.response.ErrorCode;
import {{PACKAGE}}.common.response.GlobalException;

public class UserNotFoundException extends GlobalException {
    public UserNotFoundException() {
        super(ErrorCode.USER_DATA_NOT_FOUND);
    }
}