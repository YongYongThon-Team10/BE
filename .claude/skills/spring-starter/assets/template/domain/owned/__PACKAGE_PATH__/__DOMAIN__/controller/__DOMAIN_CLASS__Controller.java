package {{PACKAGE}}.{{DOMAIN}}.controller;

import {{PACKAGE}}.common.response.GlobalResponse;
import {{PACKAGE}}.common.security.SecurityUtil;
import {{PACKAGE}}.{{DOMAIN}}.data.dto.{{DOMAIN_CLASS}}RequestDto;
import {{PACKAGE}}.{{DOMAIN}}.data.dto.{{DOMAIN_CLASS}}ResponseDto;
import {{PACKAGE}}.{{DOMAIN}}.service.{{DOMAIN_CLASS}}Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{{DOMAIN}}")
@RequiredArgsConstructor
public class {{DOMAIN_CLASS}}Controller {
    private final {{DOMAIN_CLASS}}Service {{DOMAIN}}Service;

    @PostMapping
    public GlobalResponse<Void> create{{DOMAIN_CLASS}}(@Valid @RequestBody {{DOMAIN_CLASS}}RequestDto {{DOMAIN}}RequestDto) {
        return {{DOMAIN}}Service.create{{DOMAIN_CLASS}}(SecurityUtil.getUsername(), {{DOMAIN}}RequestDto);
    }

    @GetMapping
    public GlobalResponse<{{DOMAIN_CLASS}}ResponseDto> get{{DOMAIN_CLASS}}(@RequestParam Long id) {
        return {{DOMAIN}}Service.get{{DOMAIN_CLASS}}(id);
    }
}
