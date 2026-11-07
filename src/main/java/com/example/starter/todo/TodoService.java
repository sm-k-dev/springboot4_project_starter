package com.example.starter.todo;

import com.example.starter.common.BusinessException;
import com.example.starter.common.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 할 일에 관한 업무 로직을 담당합니다. (Service 계층)
 *
 * 누가 호출하나요?
 *   - TodoController 가 호출합니다.
 *
 * 무엇을 호출하나요?
 *   - TodoRepository 로 DB를 다룹니다.
 *
 * 왜 Controller에 로직을 쓰지 않나요?
 *   - 업무 규칙을 Service에 모으면, 웹(HTTP)이 아닌 곳에서 같은 규칙을 다시 쓸 수 있고
 *     테스트도 쉬워집니다. (TodoServiceTest 참고)
 */
// @Service: "업무 로직을 담당하는 Bean"이라는 표시입니다. Spring이 자동으로 만들어 관리합니다.
@Service
public class TodoService {

    /** DB 접근 도구. 생성자로 받습니다(생성자 주입). */
    private final TodoRepository todoRepository;

    /**
     * 생성자 주입: Spring이 TodoRepository 객체를 만들어서 여기에 넣어 줍니다.
     * 필드에 @Autowired 를 붙이는 방식보다 테스트할 때 가짜(mock) 객체를 넣기 쉽습니다.
     */
    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    /**
     * 새 할 일을 저장하고 결과를 돌려줍니다. 완료 여부는 항상 false 로 시작합니다.
     * TodoController.create 에서 호출합니다.
     */
    // @Transactional: 이 메서드 안의 DB 작업을 "한 묶음"으로 묶습니다.
    // 중간에 실패하면 모두 취소(롤백)되어 데이터가 반쯤만 저장되는 일을 막습니다.
    @Transactional
    public TodoResponse create(String title) {
        TodoEntity saved = todoRepository.save(new TodoEntity(title));
        return TodoResponse.from(saved);
    }

    /**
     * id로 할 일 한 개를 찾습니다. 없으면 TODO_NOT_FOUND 예외를 던집니다.
     * TodoController.findOne 에서 호출합니다.
     */
    // readOnly = true: "이 메서드는 데이터를 바꾸지 않는다"고 알려 줍니다. 조회 성능을 조금 높여 줍니다.
    @Transactional(readOnly = true)
    public TodoResponse findById(Long id) {
        return TodoResponse.from(findOrThrow(id));
    }

    /**
     * 저장된 할 일 전체 목록을 돌려줍니다. TodoController.findAll 에서 호출합니다.
     */
    @Transactional(readOnly = true)
    public List<TodoResponse> findAll() {
        // stream().map(...).toList(): 각 Entity를 Response로 바꿔서 새 목록을 만듭니다.
        return todoRepository.findAll().stream()
                .map(TodoResponse::from)
                .toList();
    }

    /**
     * 할 일의 제목과 완료 여부를 전체 교체합니다(PUT). 없는 id 면 TODO_NOT_FOUND 예외를 던집니다.
     * TodoController.update 에서 호출합니다.
     */
    // 변경 메서드이므로 readOnly 를 끈 기본 @Transactional 을 씁니다.
    // Entity 의 값을 바꾸면 트랜잭션이 끝날 때 JPA가 변경분을 자동으로 DB에 반영합니다. (save 호출이 따로 필요 없습니다)
    @Transactional
    public TodoResponse update(Long id, String title, boolean completed) {
        TodoEntity entity = findOrThrow(id);
        entity.update(title, completed);
        return TodoResponse.from(entity);
    }

    /**
     * 할 일을 삭제합니다. 없는 id 면 TODO_NOT_FOUND 예외를 던집니다.
     * TodoController.delete 에서 호출합니다.
     * 없는 것을 먼저 확인하는 이유: 없는 id 삭제도 404로 알려 주기 위해서입니다. (조용히 넘어가면 클라이언트가 실패를 알 수 없습니다)
     */
    @Transactional
    public void delete(Long id) {
        todoRepository.delete(findOrThrow(id));
    }

    /**
     * 조회·수정·삭제가 공통으로 쓰는 "찾기 또는 실패" 도우미입니다.
     * 중복을 줄이기 위해 한곳에 모았습니다. 없으면 TODO_NOT_FOUND 를 던지고, 이 예외는 404가 됩니다.
     */
    private TodoEntity findOrThrow(Long id) {
        // Optional: "값이 있을 수도, 없을 수도 있음"을 표현하는 상자입니다. 없으면 orElseThrow 가 예외를 던집니다.
        return todoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));
    }
}
