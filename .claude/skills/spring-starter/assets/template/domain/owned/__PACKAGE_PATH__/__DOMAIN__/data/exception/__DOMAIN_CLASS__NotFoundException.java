package {{PACKAGE}}.{{DOMAIN}}.data.exception;

import {{PACKAGE}}.common.response.ErrorCode;
import {{PACKAGE}}.common.response.GlobalException;

public class {{DOMAIN_CLASS}}NotFoundException extends GlobalException {
    public {{DOMAIN_CLASS}}NotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }
}