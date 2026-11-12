package {{PACKAGE}}.{{DOMAIN}}.data.repository;

import {{PACKAGE}}.{{DOMAIN}}.data.entity.{{DOMAIN_CLASS}};
import org.springframework.data.jpa.repository.JpaRepository;

public interface {{DOMAIN_CLASS}}Repository extends JpaRepository<{{DOMAIN_CLASS}}, Long> {
}
