package com.example.starter.sample;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * SampleController 의 HTTP 동작을 확인하는 테스트입니다. (요청 → 응답 모양, 상태 코드, 검증 실패, 공통 오류)
 *
 * 왜 @WebMvcTest 인가요?
 *   - 웹 계층(Controller, 검증, 예외 처리)만 띄우고, Service/DB 는 띄우지 않습니다.
 *   - Service 는 @MockitoBean 으로 가짜를 넣어 "웹 부분만" 따로 검사합니다.
 *
 * MockMvc 란?
 *   - 실제 서버를 켜지 않고, 가상의 HTTP 요청을 Controller 에 보내서 응답을 검사하는 도구입니다.
 */
// @WebMvcTest: 웹 관련 빈만 띄웁니다. 대상 Controller 를 지정하면 그 Controller 만 띄웁니다.
@WebMvcTest(SampleController.class)
class SampleControllerTest {

    // 가짜 MockMvc: 아래에서 perform(...)으로 HTTP 요청을 보냅니다.
    @Autowired
    private MockMvc mockMvc;

    // @MockitoBean: Spring 컨텍스트에 "가짜 SampleService"를 등록합니다. Controller 가 이 가짜를 주입받습니다.
    @MockitoBean
    private SampleService sampleService;

    /**
     * 정상 요청이면 201 Created 와 생성된 데이터가 돌아오는지 확인합니다.
     */
    @Test
    void createReturns201WithBody() throws Exception {
        given(sampleService.create("첫 샘플")).willReturn(new SampleResponse(1L, "첫 샘플"));

        mockMvc.perform(post("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"첫 샘플\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("첫 샘플"));
    }

    /**
     * name 이 비어 있으면 검증(@NotBlank)에 걸려 400과 COMMON-001 코드가 돌아오는지 확인합니다.
     * 검증이 먼저 실패하므로 Service 는 호출되지 않아야 하는데, verify(never()) 로 그 사실을 실제로 확인합니다.
     */
    @Test
    void createWithBlankNameReturns400AndSkipsService() throws Exception {
        mockMvc.perform(post("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));

        verify(sampleService, never()).create(any());
    }

    /**
     * 이름이 100자를 넘으면(101자) 요청 단계에서 400으로 막히는지 확인합니다.
     * 이 검사가 없으면 DB 저장 단계에서 500이 났을 것입니다. (설계 리뷰 H-2)
     */
    @Test
    void createWithNameOver100CharsReturns400AndSkipsService() throws Exception {
        String tooLong = "가".repeat(101);

        mockMvc.perform(post("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));

        verify(sampleService, never()).create(any());
    }

    /**
     * 이름이 정확히 100자이면 경계값이므로 통과해서 201이 돌아오는지 확인합니다.
     * 위의 101자 테스트와 짝을 이루어, "100자까지 허용, 101자부터 거절"이라는 경계를 확인합니다.
     */
    @Test
    void createWithExactly100CharsReturns201() throws Exception {
        String maxLength = "가".repeat(100);
        given(sampleService.create(maxLength)).willReturn(new SampleResponse(2L, maxLength));

        mockMvc.perform(post("/api/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + maxLength + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    /**
     * 존재하지 않는 id 로 조회하면 404와 SAMPLE-001 코드가 돌아오는지 확인합니다.
     * 가짜 Service 가 "없다"는 예외를 던지도록 미리 정해 둡니다.
     */
    @Test
    void findOneMissingReturns404() throws Exception {
        given(sampleService.findById(99L)).willThrow(new BusinessException(ErrorCode.SAMPLE_NOT_FOUND));

        mockMvc.perform(get("/api/samples/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SAMPLE-001"));
    }

    /**
     * id 자리에 숫자가 아닌 값이 오면 400과 COMMON-001 코드가 돌아오는지 확인합니다. (타입 불일치)
     */
    @Test
    void findOneWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/samples/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-001"));
    }

    /**
     * Content-Type 이 JSON이 아니면 415와 COMMON-003 코드가 돌아오는지 확인합니다.
     */
    @Test
    void createWithNonJsonContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/samples")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=abc"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-003"));
    }

    /**
     * 목록 주소에 지원하지 않는 메서드(DELETE)로 요청하면 405와 COMMON-002 코드가 돌아오는지 확인합니다.
     */
    @Test
    void deleteOnListReturns405() throws Exception {
        mockMvc.perform(delete("/api/samples"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-002"));
    }

    /**
     * 존재하지 않는 주소로 요청하면 404와 COMMON-004 코드가 돌아오는지 확인합니다.
     * 샘플 없음(SAMPLE-001)과 구분되는 코드인지도 함께 봅니다.
     */
    @Test
    void unknownPathReturns404WithCommonCode() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-004"));
    }

    /**
     * Service 가 예상하지 못한 예외를 던지면 500과 COMMON-500 코드가 돌아오는지 확인합니다.
     * 응답에 내부 예외 메시지("boom")가 노출되지 않는지도 함께 확인합니다.
     */
    @Test
    void unexpectedExceptionReturns500WithoutInternalDetails() throws Exception {
        given(sampleService.findAll()).willThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON-500"))
                .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다"));
    }

    /**
     * 전체 목록 조회가 200과 목록을 돌려주는지 확인합니다.
     */
    @Test
    void findAllReturns200WithList() throws Exception {
        given(sampleService.findAll()).willReturn(List.of(new SampleResponse(1L, "A")));

        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("A"));
    }
}
