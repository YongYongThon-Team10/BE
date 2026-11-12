package {{PACKAGE}}.{{DOMAIN}}.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {{DOMAIN_CLASS}}RequestDto {
    @NotBlank
    private String name;

    @NotBlank
    private String description;
}
