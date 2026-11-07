package com.example.starter.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.starter.common.BusinessException;
import com.example.starter.common.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TodoService 의 업무 로직을 확인하는 "단위 테스트"입니다.
 *
 * 핵심 아이디어: DB(Repository)를 "가짜(Mock)"로 바꿔서, 서비스 코드만 따로 검사합니다.
 *   - 실제 DB가 없어도 동작을 확인할 수 있고, 실행이 빠릅니다.
 *
 * 누가 실행하나요?
 *   - ./gradlew test 가 자동으로 실행합니다.
 */
// MockitoExtension: @Mock 으로 표시된 가짜 객체를 테스트 시작 전에 만들어 주는 확장 기능입니다.
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    // @Mock: 진짜 TodoRepository 대신 "가짜"를 만듭니다. 가짜가 무엇을 돌려줄지는 given(...)으로 정합니다.
    @Mock
    private TodoRepository todoRepository;

    // @InjectMocks: 테스트 대상(TodoService)을 만들면서, 위의 가짜 Repository를 생성자에 넣어 줍니다.
    @InjectMocks
    private TodoService todoService;

    /**
     * create 는 새 할 일을 미완료(completed = false)로 저장하고, 그 결과를 응답으로 돌려주는지 확인합니다.
     * 완료 여부를 요청으로 받지 않으므로, 저장되는 엔터티의 completed 가 false 인지가 핵심입니다.
     */
    @Test
    void createSavesIncompleteTodo() {
        // 가짜 Repository 가 save 에 넘어온 엔터티를 그대로 돌려주도록 약속합니다.
        // (실제 id 발급은 DB가 하므로 TodoRepositoryTest 에서 확인합니다)
        given(todoRepository.save(any(TodoEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoService.create("우유 사기");

        assertThat(response.title()).isEqualTo("우유 사기");
        assertThat(response.completed()).isFalse();
    }

    /**
     * 존재하는 id 로 조회하면 해당 할 일이 돌아오는지 확인합니다.
     */
    @Test
    void findByIdReturnsTodoWhenExists() {
        // 실제 DB 없이 Entity 를 직접 만들어 가짜 Repository 가 돌려주게 합니다.
        given(todoRepository.findById(1L)).willReturn(Optional.of(new TodoEntity("조회용")));

        TodoResponse response = todoService.findById(1L);

        assertThat(response.title()).isEqualTo("조회용");
    }

    /**
     * 없는 id 로 조회하면 TODO_NOT_FOUND 예외가 던져지는지 확인합니다.
     * 이 예외가 GlobalExceptionHandler 를 거쳐 404가 됩니다.
     */
    @Test
    void findByIdThrowsWhenMissing() {
        given(todoRepository.findById(99L)).willReturn(Optional.empty());

        // assertThatExceptionOfType: 던져진 예외의 타입을 확인하고, 그 예외 안의 ErrorCode 를 꺼내 비교합니다.
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> todoService.findById(99L))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.TODO_NOT_FOUND);
    }

    /**
     * 전체 목록 조회가 Repository 의 결과를 모두 응답 목록으로 옮기는지 확인합니다.
     */
    @Test
    void findAllReturnsAllTodos() {
        given(todoRepository.findAll()).willReturn(List.of(
                new TodoEntity("A"),
                new TodoEntity("B")));

        List<TodoResponse> responses = todoService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).title()).isEqualTo("A");
        assertThat(responses.get(1).title()).isEqualTo("B");
    }

    /**
     * 수정하면 엔터티의 제목과 완료 여부가 새 값으로 바뀌어 돌아오는지 확인합니다.
     */
    @Test
    void updateChangesTitleAndCompleted() {
        TodoEntity existing = new TodoEntity("우유 사기");
        given(todoRepository.findById(1L)).willReturn(Optional.of(existing));

        TodoResponse response = todoService.update(1L, "우유 두 개 사기", true);

        assertThat(response.title()).isEqualTo("우유 두 개 사기");
        assertThat(response.completed()).isTrue();
    }

    /**
     * 없는 id 를 수정하려 하면 TODO_NOT_FOUND 예외가 던져지고, 저장은 일어나지 않는지 확인합니다.
     */
    @Test
    void updateThrowsWhenMissing() {
        given(todoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> todoService.update(99L, "수정", false))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.TODO_NOT_FOUND);
    }

    /**
     * 있는 id 를 삭제하면 Repository 의 delete 가 그 엔터티로 호출되는지 확인합니다.
     */
    @Test
    void deleteRemovesExistingTodo() {
        TodoEntity existing = new TodoEntity("지울 것");
        given(todoRepository.findById(1L)).willReturn(Optional.of(existing));

        todoService.delete(1L);

        verify(todoRepository).delete(existing);
    }

    /**
     * 없는 id 를 삭제하려 하면 TODO_NOT_FOUND 예외가 던져지는지 확인합니다.
     * 조용히 넘어가지 않고 404로 알려 주는 것이 설계 방침입니다.
     */
    @Test
    void deleteThrowsWhenMissing() {
        given(todoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> todoService.delete(99L))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.TODO_NOT_FOUND);
    }
}
