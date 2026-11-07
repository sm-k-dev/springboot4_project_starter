package com.example.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 app.* 설정 값을 자바 객체로 담는 클래스입니다.
 *
 * 누가 사용하나요?
 *   - Spring이 시작할 때 자동으로 값을 채워 넣습니다. 필요한 곳에서 생성자 주입으로 받아 씁니다.
 *   - 예: StarterApplicationTests 가 app.name 값을 확인합니다.
 *
 * 왜 record를 쓰나요?
 *   - record는 필드와 생성자, getter를 자동으로 만들어 주는 "값 보관용" 문법입니다.
 *   - 설정 값은 한 번 정해지면 바뀌지 않으므로 record가 잘 맞습니다.
 *
 * 왜 @ConfigurationProperties 를 쓰나요?
 *   - "@Value("${app.name}")" 를 여기저기 흩뿌리지 않고, 설정 값을 한곳에 모아 관리하기 위해서입니다.
 */
// prefix = "app": yml에서 "app:" 아래에 있는 값들을 읽겠다는 뜻입니다.
// 여기서 name 은 yml의 app.name 과 자동으로 연결됩니다.
@ConfigurationProperties(prefix = "app")
public record AppProperties(String name) {
}
