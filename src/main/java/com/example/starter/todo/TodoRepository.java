package com.example.starter.todo;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 할 일 DB 접근만 담당하는 인터페이스입니다. (Repository 계층)
 *
 * 누가 호출하나요?
 *   - TodoService 만 호출합니다. Controller는 여기를 직접 부르지 않습니다.
 *
 * 왜 구현 클래스를 만들지 않나요?
 *   - JpaRepository를 상속하면 save, findById, findAll, delete 등의 실제 구현을 Spring Data JPA가 자동으로 만들어 줍니다.
 *     그래서 이 인터페이스에는 추가 메서드를 쓰지 않습니다.
 *
 * 제네릭 설명: <TodoEntity, Long> = "TodoEntity 를 다루고, 기본 키 타입은 Long"이라는 뜻입니다.
 */
// 인터페이스에는 @Repository 를 따로 붙이지 않아도 Spring이 자동으로 찾아 등록합니다.
public interface TodoRepository extends JpaRepository<TodoEntity, Long> {
}
