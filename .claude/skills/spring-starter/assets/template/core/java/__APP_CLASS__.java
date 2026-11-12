package {{PACKAGE}};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class {{APP_CLASS}} {

    public static void main(String[] args) {
        // 빈 생성 전에 JVM 기본 시간대를 고정해 LocalDateTime.now() 등이 서버 위치와 무관하게 같은 시간대를 쓰게 한다
        TimeZone.setDefault(TimeZone.getTimeZone("{{TIMEZONE}}"));
        SpringApplication.run({{APP_CLASS}}.class, args);
    }

}
