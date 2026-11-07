package com.example.starter.todo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * DB 테이블 "todos" 의 한 행(row)을 자바 객체로 표현한 클래스입니다. (JPA 엔터티)
 *
 * 누가 사용하나요?
 *   - TodoRepository 가 저장/조회할 때 사용합니다.
 *   - TodoService 가 TodoResponse 로 바꿔서 Controller에 넘깁니다.
 *
 * 왜 API 응답에 직접 쓰지 않나요?
 *   - DB 구조가 바뀌어도 API 모양은 그대로 유지하기 위해서입니다. 응답은 TodoResponse 로 따로 만듭니다.
 *
 * 왜 setter가 없나요?
 *   - 값은 생성자로 정하고, 수정은 update 메서드 하나로만 허용합니다. 아무 곳에서나 값이 바뀌는 것을 막습니다.
 */
// @Entity: "이 클래스는 DB 테이블과 연결된다"는 표시입니다.
@Entity
// @Table: 연결할 테이블 이름을 "todos"로 정합니다.
@Table(name = "todos")
public class TodoEntity {

    // @Id: 기본 키(행을 구별하는 번호)입니다.
    @Id
    // IDENTITY: DB가 1, 2, 3... 번호를 자동으로 매기도록 합니다. (H2에서도 동작합니다)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: 컬럼 제약 조건입니다. nullable=false 는 "반드시 값이 있어야 함", length=100 은 최대 글자 수입니다.
    // 입력 검증(@Size(max = 100))과 같은 기준이라, 검증을 통과한 제목은 DB 길이 제한에 걸리지 않습니다.
    @Column(nullable = false, length = 100)
    private String title;

    // 완료 여부입니다. 새 할 일은 항상 false(미완료)로 시작합니다. 빠지면 안 되므로 nullable=false 입니다.
    @Column(nullable = false)
    private boolean completed;

    /**
     * JPA는 DB에서 객체를 만들 때 "기본 생성자(인자 없는 생성자)"가 필요합니다.
     * protected 로 두어 우리 코드에서는 실수로 쓰지 않도록 합니다.
     */
    protected TodoEntity() {
    }

    /**
     * 새 할 일을 만들 때 호출합니다. TodoService.create 에서 사용합니다.
     * id 는 아직 없습니다. DB에 저장(save)되면서 자동으로 번호가 붙습니다.
     * 새 할 일은 항상 미완료(completed = false)로 시작합니다. 완료 여부는 생성 요청으로 받지 않습니다.
     */
    public TodoEntity(String title) {
        this.title = title;
        this.completed = false;
    }

    /**
     * 할 일의 제목과 완료 여부를 한 번에 바꿉니다. (PUT 전체 교체 방식)
     * 호출처: TodoService.update 가 검증을 통과한 값으로 호출합니다.
     * 왜 setter 대신 이 메서드인가요? 수정 규칙을 이 한곳에 모아 두기 위해서입니다.
     */
    public void update(String title, boolean completed) {
        this.title = title;
        this.completed = completed;
    }

    /**
     * 기본 키 번호를 돌려줍니다. 아직 저장하기 전이면 null 입니다.
     * 호출처: TodoResponse.from 이 응답의 id 칸에 넣을 때, TodoRepositoryTest 가 저장 후 확인할 때 씁니다.
     */
    public Long getId() {
        return id;
    }

    /**
     * 할 일 제목을 돌려줍니다. 호출처: TodoResponse.from 이 응답의 title 칸에 넣을 때 씁니다.
     */
    public String getTitle() {
        return title;
    }

    /**
     * 완료 여부를 돌려줍니다. 호출처: TodoResponse.from 이 응답의 completed 칸에 넣을 때 씁니다.
     */
    public boolean isCompleted() {
        return completed;
    }
}
