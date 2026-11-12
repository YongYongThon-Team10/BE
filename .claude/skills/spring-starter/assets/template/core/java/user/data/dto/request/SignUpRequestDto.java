package {{PACKAGE}}.user.data.dto.request;

{{SIGNUP_ROLE_ENUM_IMPORT}}import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;{{SIGNUP_ROLE_NOTNULL_IMPORT}}
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;{{SIGNUP_ROLE_FIELD}}
}
