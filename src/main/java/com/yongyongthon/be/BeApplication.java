package com.yongyongthon.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BeApplication {

    public static void main(String[] args) {
        // 빈 생성 전에 JVM 기본 시간대를 고정해 LocalDateTime.now() 등이 서버 위치와 무관하게 같은 시간대를 쓰게 한다
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(BeApplication.class, args);
    }

}
