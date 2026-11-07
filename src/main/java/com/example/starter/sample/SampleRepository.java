package com.example.starter.sample;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DB 접근만 담당하는 인터페이스입니다. (Repository 계층)
 *
 * 누가 호출하나요?
 *   - SampleService 만 호출합니다. Controller는 여기를 직접 부르지 않습니다.
 *
 * 왜 구현 클래스를 만들지 않나요?
 *   - JpaRepository를 상속하면 Spring Data JPA가 save, findById, findAll 같은 메서드의
 *     실제 구현을 자동으로 만들어 줍니다. 우리는 "이런 인터페이스가 필요하다"만 선언하면 됩니다.
 *
 * 제네릭 설명: <SampleEntity, Long> = "SampleEntity 를 다루고, 기본 키 타입은 Long"이라는 뜻입니다.
 */
// 인터페이스에는 @Repository 를 따로 붙이지 않아도 Spring이 자동으로 찾아 등록합니다.
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
}
