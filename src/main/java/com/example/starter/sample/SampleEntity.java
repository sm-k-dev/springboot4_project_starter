package com.example.starter.sample;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * DB 테이블 "samples" 의 한 행(row)을 자바 객체로 표현한 클래스입니다. (JPA 엔터티)
 *
 * 누가 사용하나요?
 *   - SampleRepository 가 저장/조회할 때 사용합니다.
 *   - SampleService 가 SampleResponse 로 바꿔서 Controller에 넘깁니다.
 *
 * 왜 API 응답에 직접 쓰지 않나요?
 *   - DB 구조가 바뀌어도 API 모양은 그대로 유지하기 위해서입니다. 응답은 SampleResponse 로 따로 만듭니다.
 *
 * 왜 setter가 없나요?
 *   - 값은 생성자로 한 번에 정하고, 나중에 몰래 바뀌지 않도록 막습니다.
 */
// @Entity: "이 클래스는 DB 테이블과 연결된다"는 표시입니다.
@Entity
// @Table: 연결할 테이블 이름을 "samples"로 정합니다. (생략하면 클래스 이름 SampleEntity 가 테이블 이름이 됩니다)
@Table(name = "samples")
public class SampleEntity {

    // @Id: 기본 키(행을 구별하는 번호)입니다.
    @Id
    // IDENTITY: DB가 1, 2, 3... 번호를 자동으로 매기도록 합니다. (H2에서도 동작합니다)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: 컬럼 제약 조건입니다. nullable=false 는 "반드시 값이 있어야 함", length=100 은 최대 글자 수입니다.
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * JPA는 DB에서 객체를 만들 때 "기본 생성자(인자 없는 생성자)"가 필요합니다.
     * 그래서 빈 생성자를 하나 둡니다. protected 로 두어 우리 코드에서는 실수로 쓰지 않도록 합니다.
     */
    protected SampleEntity() {
    }

    /**
     * 새 샘플을 만들 때 호출합니다. SampleService.create 에서 사용합니다.
     * id 는 아직 없습니다. DB에 저장(save)되면서 자동으로 번호가 붙습니다.
     */
    public SampleEntity(String name) {
        this.name = name;
    }

    /**
     * 기본 키 번호를 돌려줍니다. 아직 저장하기 전이면 null 입니다.
     * 호출처: SampleResponse.from 이 응답의 id 칸에 넣을 때, SampleRepository 테스트가 저장 후 확인할 때 씁니다.
     */
    public Long getId() {
        return id;
    }

    /**
     * 샘플 이름을 돌려줍니다.
     * 호출처: SampleResponse.from 이 응답의 name 칸에 넣을 때 씁니다.
     */
    public String getName() {
        return name;
    }
}
