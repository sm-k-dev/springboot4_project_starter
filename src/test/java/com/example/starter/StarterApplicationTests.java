package com.example.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.starter.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 스프링 전체가 정상으로 뜨는지 확인하는 "통합 테스트"입니다.
 *
 * 누가 실행하나요?
 *   - ./gradlew test 를 실행하면 JUnit이 자동으로 찾아서 실행합니다.
 *
 * 왜 필요한가요?
 *   - Bean 등록 실패, 설정 파일 오타, 잘못된 DB 설정 같은 문제는 "앱을 실제로 띄워야" 드러납니다.
 *     contextLoads()가 통과하면 "앱이 기동된다"는 가장 기본적인 보증이 됩니다.
 */
// @SpringBootTest: 실제 앱과 똑같이 Spring 전체를 띄워서 테스트합니다. (느리지만 현실에 가깝습니다)
// 참고: Spring Boot 4에서는 MockMvc가 자동으로 만들어지지 않습니다. 이 테스트는 MockMvc가 필요 없어서 그대로 둡니다.
@SpringBootTest
class StarterApplicationTests {

    // @Autowired: 테스트 대상이 되는 Bean을 Spring에서 꺼내 옵니다. (테스트 클래스에서는 필드 주입을 씁니다)
    @Autowired
    private AppProperties appProperties;

    /**
     * 아무 검사 없이 빈 메서드입니다. 위의 @SpringBootTest 가 컨텍스트를 띄우는 데 성공하면 통과합니다.
     * 실패하면 "Application context failed to load" 같은 오류가 납니다.
     */
    @Test
    void contextLoads() {
    }

    /**
     * application.yml 의 app.name 값(starter)이 AppProperties 에 제대로 들어왔는지 확인합니다.
     * 설정 파일과 자바 클래스의 연결이 끊기면 이 테스트가 실패합니다.
     */
    @Test
    void appPropertiesAreBoundFromYaml() {
        assertThat(appProperties.name()).isEqualTo("starter");
    }
}
