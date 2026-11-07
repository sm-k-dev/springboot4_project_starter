package com.example.starter.todo;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * 실제 DB(H2)에 저장·수정·삭제하고 Repository 가 정상 동작하는지 확인하는 테스트입니다.
 *
 * 왜 @DataJpaTest 인가요?
 *   - JPA 관련 부분(Entity, Repository, H2 DB)만 띄우므로 전체 앱보다 빠르게 실행됩니다.
 *   - 테스트가 끝나면 각 테스트의 변경 사항은 자동으로 롤백되어, 테스트끼리 서로 영향을 주지 않습니다.
 *
 * 왜 flush() 와 clear() 를 부르나요?
 *   - JPA는 같은 트랜잭션 안에서 방금 저장한 객체를 메모리(영속성 컨텍스트)에 들고 있다가 그대로 돌려줍니다.
 *     그러면 DB를 거치지 않은 값을 보게 되어 "DB에 실제로 반영됐다"는 증명이 안 됩니다.
 *   - flush() 는 쌓인 변경을 DB로 밀어 넣고, clear() 는 메모리 캐시를 비웁니다.
 *     두 호출 뒤에 조회하면 값이 실제 DB에서 다시 읽힌 것입니다.
 */
// @DataJpaTest: DB 관련 빈만 띄우는 테스트 애노테이션입니다. (패키지 경로는 Spring Boot 4 기준입니다)
@DataJpaTest
class TodoRepositoryTest {

    // 실제 Repository 객체를 Spring에서 꺼내 씁니다. (가짜가 아닌 진짜 DB 접근)
    @Autowired
    private TodoRepository todoRepository;

    // EntityManager: JPA의 "작업 담당자"입니다. flush()와 clear()로 메모리 캐시를 조작할 때 씁니다.
    @Autowired
    private EntityManager entityManager;

    /**
     * 저장(save)하면 DB가 id를 자동으로 붙여 주고, 캐시를 비운 뒤에도 같은 내용을 DB에서 다시 꺼낼 수 있는지 확인합니다.
     * 새로 만든 할 일은 completed 가 false 여야 합니다.
     */
    @Test
    void saveAndFindByIdReadsFromDatabase() {
        TodoEntity saved = todoRepository.save(new TodoEntity("DB 테스트"));

        // save 가 끝나면 IDENTITY 전략에 따라 DB가 발급한 id 가 채워져 있어야 합니다.
        assertThat(saved.getId()).isNotNull();

        // 쌓인 변경을 DB로 보내고, 메모리 캐시를 비웁니다. 이후 조회는 반드시 DB에서 읽습니다.
        entityManager.flush();
        entityManager.clear();

        TodoEntity found = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("DB 테스트");
        assertThat(found.isCompleted()).isFalse();
    }

    /**
     * update 로 바꾼 제목과 완료 여부가 DB에 반영되어, 캐시를 비운 뒤에도 새 값으로 읽히는지 확인합니다.
     */
    @Test
    void updateIsReflectedInDatabase() {
        TodoEntity saved = todoRepository.save(new TodoEntity("수정 전"));
        entityManager.flush();

        saved.update("수정 후", true);
        entityManager.flush();
        entityManager.clear();

        TodoEntity found = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("수정 후");
        assertThat(found.isCompleted()).isTrue();
    }

    /**
     * 삭제한 뒤에는 같은 id 로 조회해도 아무것도 나오지 않는지 확인합니다.
     */
    @Test
    void deletedTodoCannotBeFound() {
        TodoEntity saved = todoRepository.save(new TodoEntity("지울 것"));
        Long id = saved.getId();
        entityManager.flush();

        todoRepository.delete(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(todoRepository.findById(id)).isEmpty();
    }

    /**
     * 여러 개를 저장한 뒤 findAll 로 전부 가져올 수 있는지 확인합니다.
     * 테스트 DB는 매번 비어 있는 상태에서 시작하므로 개수를 정확히 비교할 수 있습니다.
     */
    @Test
    void findAllReturnsEverySavedRow() {
        todoRepository.save(new TodoEntity("첫째"));
        todoRepository.save(new TodoEntity("둘째"));

        entityManager.flush();
        entityManager.clear();

        assertThat(todoRepository.findAll()).hasSize(2);
    }
}
