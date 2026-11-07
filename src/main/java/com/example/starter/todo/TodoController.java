package com.example.starter.todo;

import com.example.starter.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 할 일 API의 입구입니다. HTTP 요청을 받아 Service에 넘기고, 결과를 응답으로 돌려줍니다.
 *
 * 제공하는 API:
 *   POST   /api/todos       할 일 생성 (201)
 *   GET    /api/todos/{id}  단건 조회 (200, 없으면 404)
 *   GET    /api/todos       전체 목록 (200)
 *   PUT    /api/todos/{id}  제목·완료 여부 전체 수정 (200, 없으면 404)
 *   DELETE /api/todos/{id}  삭제 (200, 없으면 404)
 *
 * 왜 삭제도 200 + ApiResponse 인가요?
 *   - 204(본문 없음) 대신 모든 응답을 같은 모양으로 유지하기 위해서입니다. 프론트엔드가 응답을 한 가지 방식으로만 읽으면 됩니다.
 *
 * 규칙: Controller는 "받고 넘기고 돌려주기"만 합니다. 업무 판단은 TodoService 에 맡깁니다.
 */
// @RestController: 이 클래스의 메서드 반환값을 JSON으로 자동 변환해 응답하는 Controller라는 표시입니다.
@RestController
// @RequestMapping: 이 클래스의 모든 API 주소 앞에 공통으로 붙는 경로입니다.
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    /** 생성자 주입: Spring이 TodoService 객체를 넣어 줍니다. */
    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * POST /api/todos : 할 일을 만듭니다.
     * 요청 예시: {"title": "우유 사기"}
     */
    // @PostMapping: HTTP POST 요청을 이 메서드로 연결합니다. (주소는 클래스의 /api/todos 그대로)
    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> create(
            // @RequestBody: 요청 본문 JSON을 TodoRequest 객체로 바꿔 줍니다.
            // @Valid: TodoRequest 의 @NotBlank 같은 검증을 실행합니다. 실패하면 GlobalExceptionHandler 가 400을 돌려줍니다.
            @Valid @RequestBody TodoRequest request) {
        TodoResponse created = todoService.create(request.title());
        // 새 자원이 만들어졌으므로 HTTP 상태는 200이 아니라 201(Created)을 씁니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    /**
     * GET /api/todos/{id} : id 로 할 일 한 개를 조회합니다. 없으면 Service가 던진 예외가 404로 바뀝니다.
     */
    // "/{id}" 부분은 주소에 적힌 숫자를 아래 매개변수로 넘기라는 뜻입니다. 예: /api/todos/3 → id = 3
    // @PathVariable("id"): 변수 이름을 직접 적어 둡니다. 숫자가 아닌 값(예: /api/todos/abc)이면 400(COMMON-001)이 됩니다.
    @GetMapping("/{id}")
    public ApiResponse<TodoResponse> findOne(@PathVariable("id") Long id) {
        return ApiResponse.ok(todoService.findById(id));
    }

    /**
     * GET /api/todos : 할 일 전체 목록을 조회합니다.
     */
    @GetMapping
    public ApiResponse<List<TodoResponse>> findAll() {
        return ApiResponse.ok(todoService.findAll());
    }

    /**
     * PUT /api/todos/{id} : 할 일의 제목과 완료 여부를 전체 교체합니다.
     * 요청 예시: {"title": "우유 두 개 사기", "completed": true}
     * 부분 수정(PATCH)이 아니라 전체 교체이므로, 두 값이 모두 있어야 검증을 통과합니다.
     */
    // @PutMapping: HTTP PUT 요청을 이 메서드로 연결합니다.
    @PutMapping("/{id}")
    public ApiResponse<TodoResponse> update(
            @PathVariable("id") Long id,
            // @Valid: TodoUpdateRequest 의 검증(제목 필수·100자, completed 필수)을 실행합니다.
            @Valid @RequestBody TodoUpdateRequest request) {
        // 검증을 통과했으므로 completed 는 null 이 아닙니다. 그래서 boolean 으로 풀어 넘겨도 안전합니다.
        return ApiResponse.ok(todoService.update(id, request.title(), request.completed()));
    }

    /**
     * DELETE /api/todos/{id} : 할 일을 삭제합니다. 없으면 404가 됩니다.
     * 성공 시 data 는 비어 있습니다(null). 응답 모양을 다른 API와 같게 맞추기 위한 것입니다.
     */
    // @DeleteMapping: HTTP DELETE 요청을 이 메서드로 연결합니다.
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        todoService.delete(id);
        return ApiResponse.ok(null);
    }
}
