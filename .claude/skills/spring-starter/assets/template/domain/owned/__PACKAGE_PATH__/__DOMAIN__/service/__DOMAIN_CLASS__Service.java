package {{PACKAGE}}.{{DOMAIN}}.service;

import {{PACKAGE}}.common.response.GlobalResponse;
import {{PACKAGE}}.{{DOMAIN}}.data.dto.{{DOMAIN_CLASS}}RequestDto;
import {{PACKAGE}}.{{DOMAIN}}.data.dto.{{DOMAIN_CLASS}}ResponseDto;
import {{PACKAGE}}.{{DOMAIN}}.data.entity.{{DOMAIN_CLASS}};
import {{PACKAGE}}.{{DOMAIN}}.data.exception.{{DOMAIN_CLASS}}NotFoundException;
import {{PACKAGE}}.{{DOMAIN}}.data.repository.{{DOMAIN_CLASS}}Repository;
import {{PACKAGE}}.user.data.entity.User;
import {{PACKAGE}}.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class {{DOMAIN_CLASS}}Service {
    private final {{DOMAIN_CLASS}}Repository {{DOMAIN}}Repository;
    private final AuthService authService;

    private {{DOMAIN_CLASS}} get{{DOMAIN_CLASS}}Entity(Long id) {
        return {{DOMAIN}}Repository.findById(id)
                .orElseThrow({{DOMAIN_CLASS}}NotFoundException::new);
    }

    @Transactional
    public GlobalResponse<Void> create{{DOMAIN_CLASS}}(String email, {{DOMAIN_CLASS}}RequestDto {{DOMAIN}}RequestDto) {
        User user = authService.getUserEntity(email);

        {{DOMAIN}}Repository.save(
                {{DOMAIN_CLASS}}.builder()
                        .name({{DOMAIN}}RequestDto.getName())
                        .description({{DOMAIN}}RequestDto.getDescription())
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return GlobalResponse.ok();
    }

    @Transactional(readOnly = true)
    public GlobalResponse<{{DOMAIN_CLASS}}ResponseDto> get{{DOMAIN_CLASS}}(Long id) {
        {{DOMAIN_CLASS}} {{DOMAIN}} = get{{DOMAIN_CLASS}}Entity(id);

        return GlobalResponse.ok(
                {{DOMAIN_CLASS}}ResponseDto.builder()
                        .id({{DOMAIN}}.getId())
                        .name({{DOMAIN}}.getName())
                        .description({{DOMAIN}}.getDescription())
                        .createdAt({{DOMAIN}}.getCreatedAt())
                        .build()
        );
    }
}
