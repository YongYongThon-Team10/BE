package {{PACKAGE}}.{{DOMAIN}}.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {{DOMAIN_CLASS}}ResponseDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
