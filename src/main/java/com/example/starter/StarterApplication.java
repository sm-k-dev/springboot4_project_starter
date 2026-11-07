package com.example.starter;

import com.example.starter.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 애플리케이션의 시작점입니다.
 *
 * 누가 호출하나요?
 *   - 우리가 "실행"할 때(./gradlew bootRun) Java가 가장 먼저 main 메서드를 호출합니다.
 *
 * 왜 필요한가요?
 *   - Spring은 이 클래스가 있는 패키지(com.example.starter) 아래의 클래스들을 자동으로 찾아서
 *     Controller, Service, Repository 등을 등록합니다. 그래서 이 클래스는 최상위 패키지에 둡니다.
 */
// @SpringBootApplication: "이것이 Spring Boot 앱의 시작 클래스다"라는 표시입니다.
// 안에 "설정 클래스 + 자동 설정 + 컴포넌트 검색"이 모두 들어 있습니다.
@SpringBootApplication
// @EnableConfigurationProperties: application.yml의 app.* 값을 AppProperties 객체로 묶으라고 알려 줍니다.
@EnableConfigurationProperties(AppProperties.class)
public class StarterApplication {

    /**
     * 프로그램이 실행되면 가장 먼저 호출되는 메서드입니다.
     * SpringApplication.run은 내장 서버(Tomcat)를 띄우고 모든 Bean을 준비합니다.
     */
    public static void main(String[] args) {
        SpringApplication.run(StarterApplication.class, args);
    }
}
