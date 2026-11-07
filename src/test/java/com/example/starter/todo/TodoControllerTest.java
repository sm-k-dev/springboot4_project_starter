package com.example.starter.todo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.starter.common.BusinessException;
import com.example.starter.common.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TodoController 의 HTTP 동작을 확인하는 테스트입니다. (요청 → 응답 모양, 상태 코드, 검증 실패, 공통 오류)
 *
 * 왜 @WebMvcTest 인가요?
 *   - 웹 계층(Controller, 검증, 예외 처리)만 띄우고, Service/DB 는 띄우지 않습니다.
 *   - Service 는 @MockitoBean 으로 가짜를 넣어 "웹 부분만" 따로 검사합니다.
 *
 * MockMvc 란?
 *   - 실제 서버를 켜지 않고, 가상의 HTTP 요청을 Controller 에 보내서 응답을 검사하는 도구입니다.
 */
// @WebMvcTest: 웹 관련 빈만 띄웁니다. 대상 Controller 를 지정하면 그 Controller 만 띄웁니다.
@WebMvcTest(TodoController.class)
class TodoControllerTest {

    // 가짜 MockMvc: 아래에서 perform(...)으로 HTTP 요청을 보냅니다.
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean: Spring 컨텍스트에 "가짜 TodoService"를 등록합니다. Controller 가 이 가짜를 주입받습니다.
    @MockitoBean
    private TodoService todoService;

    /**
     * 정상 생성 요청이면 201 Created 와 생성된 데이터(미완료)가 돌아오는지 확인합니다.
     */
    @Test
    void createReturns201WithBody() throws Exception {
        given(todoService.create("우유 사기")).willReturn(new TodoResponse(1L, "우유 사기", false));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"우유 사기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("우유 사기"))
                .andExpect(jsonPath("$.data.completed").value(false));
    }

    /**
     * title 이 공백뿐이면 검증(@NotBlank)에 걸려 400과 COMMON-001 코드가 돌아오는지 확인합니다.
     * 검증이 먼저 실패하므로 Service 는 호출되지 않아야 하는데, verify(never()) 로 그 사실을 실제로 확인합니다.
     */
    @Test
    void createWithBlankTitleReturns400AndSkipsService() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));

        verify(todoService, never()).create(any());
    }

    /**
     * 제목이 100자를 넘으면(101자) 요청 단계에서 400으로 막히는지 확인합니다.
     */
    @Test
    void createWithTitleOver100CharsReturns400AndSkipsService() throws Exception {
        String tooLong = "가".repeat(101);

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));

        verify(todoService, never()).create(any());
    }

    /**
     * 제목이 정확히 100자이면 경계값이므로 통과해서 201이 돌아오는지 확인합니다.
     * 위의 101자 테스트와 짝을 이루어, "100자까지 허용, 101자부터 거절"이라는 경계를 확인합니다.
     */
    @Test
    void createWithExactly100CharsReturns201() throws Exception {
        String maxLength = "가".repeat(100);
        given(todoService.create(maxLength)).willReturn(new TodoResponse(2L, maxLength, false));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + maxLength + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    /**
     * 단건 조회가 있으면 200과 할 일을 돌려주는지 확인합니다.
     */
    @Test
    void findOneReturns200WithTodo() throws Exception {
        given(todoService.findById(1L)).willReturn(new TodoResponse(1L, "A", true));

        mockMvc.perform(get("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("A"))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    /**
     * 존재하지 않는 id 로 조회하면 404와 TODO-001 코드가 돌아오는지 확인합니다.
     * 가짜 Service 가 "없다"는 예외를 던지도록 미리 정해 둡니다.
     */
    @Test
    void findOneMissingReturns404() throws Exception {
        given(todoService.findById(99L)).willThrow(new BusinessException(ErrorCode.TODO_NOT_FOUND));

        mockMvc.perform(get("/api/todos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TODO-001"));
    }

    /**
     * id 자리에 숫자가 아닌 값이 오면 400과 COMMON-001 코드가 돌아오는지 확인합니다. (타입 불일치)
     */
    @Test
    void findOneWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/todos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }

    /**
     * 전체 목록 조회가 200과 목록을 돌려주는지 확인합니다.
     */
    @Test
    void findAllReturns200WithList() throws Exception {
        given(todoService.findAll()).willReturn(List.of(new TodoResponse(1L, "A", false)));

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("A"));
    }

    /**
     * 정상 수정 요청이면 200과 수정된 할 일을 돌려주는지 확인합니다.
     */
    @Test
    void updateReturns200WithBody() throws Exception {
        given(todoService.update(1L, "우유 두 개 사기", true))
                .willReturn(new TodoResponse(1L, "우유 두 개 사기", true));

        mockMvc.perform(put("/api/todos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"우유 두 개 사기\", \"completed\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("우유 두 개 사기"))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    /**
     * 없는 id 를 수정하면 404와 TODO-001 코드가 돌아오는지 확인합니다.
     */
    @Test
    void updateMissingReturns404() throws Exception {
        given(todoService.update(99L, "수정", false)).willThrow(new BusinessException(ErrorCode.TODO_NOT_FOUND));

        mockMvc.perform(put("/api/todos/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"수정\", \"completed\": false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TODO-001"));
    }

    /**
     * 수정 요청에서 completed 가 빠지면 검증(@NotNull)에 걸려 400과 COMMON-001이 돌아오는지 확인합니다.
     * PUT 은 전체 교체이므로 두 값이 모두 필요합니다. Service 는 호출되지 않아야 합니다.
     */
    @Test
    void updateWithoutCompletedReturns400AndSkipsService() throws Exception {
        mockMvc.perform(put("/api/todos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"우유 사기\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));

        verify(todoService, never()).update(any(), any(), anyBoolean());
    }

    /**
     * 수정 요청의 제목이 공백뿐이면 400과 COMMON-001이 돌아오는지 확인합니다.
     */
    @Test
    void updateWithBlankTitleReturns400() throws Exception {
        mockMvc.perform(put("/api/todos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"  \", \"completed\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }

    /**
     * 숫자가 아닌 id 로 수정하면 400과 COMMON-001이 돌아오는지 확인합니다.
     */
    @Test
    void updateWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(put("/api/todos/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"수정\", \"completed\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }

    /**
     * 삭제가 성공하면 200과 success=true 가 돌아오고, data 는 비어 있는지 확인합니다.
     */
    @Test
    void deleteReturns200WithEmptyData() throws Exception {
        mockMvc.perform(delete("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 없는 id 를 삭제하면 404와 TODO-001 코드가 돌아오는지 확인합니다.
     */
    @Test
    void deleteMissingReturns404() throws Exception {
        willThrow(new BusinessException(ErrorCode.TODO_NOT_FOUND))
                .given(todoService).delete(99L);

        mockMvc.perform(delete("/api/todos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TODO-001"));
    }

    /**
     * 수정 요청의 제목이 101자이면 생성과 같은 규칙으로 400이 돌아오는지 확인합니다.
     */
    @Test
    void updateWithTitleOver100CharsReturns400() throws Exception {
        String tooLong = "가".repeat(101);

        mockMvc.perform(put("/api/todos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + tooLong + "\", \"completed\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }
}
