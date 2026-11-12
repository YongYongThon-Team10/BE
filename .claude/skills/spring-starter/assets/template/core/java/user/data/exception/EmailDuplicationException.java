package {{PACKAGE}}.user.data.exception;

import {{PACKAGE}}.common.response.ErrorCode;
import {{PACKAGE}}.common.response.GlobalException;

public class EmailDuplicationException extends GlobalException {
    public EmailDuplicationException() {
        super(ErrorCode.EMAIL_DUPLICATION);
    }
}
