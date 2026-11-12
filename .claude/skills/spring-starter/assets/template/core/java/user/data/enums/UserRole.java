package {{PACKAGE}}.user.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
{{ROLE_ENUM_VALUES}}

    private final String value;
}
