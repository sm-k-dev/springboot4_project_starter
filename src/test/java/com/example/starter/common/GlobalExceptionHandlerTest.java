package com.example.starter.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * GlobalExceptionHandler 의 처리기 8개를 하나씩 확인하는 "단위 테스트"입니다.
 *
 * 왜 @WebMvcTest 대신 평범한 new 로 만드나요?
 *   - 처리기는 예외 객체를 받아 응답 객체를 돌려주기만 합니다. 스프링을 띄우지 않고 직접 호출하면 빠르고 단순합니다.
 *   - HTTP를 통해 실제로 발생시키는 경로(예: 잘못된 JSON, 없는 주소)는 SampleControllerTest 에서 함께 확인합니다.
 *
 * 각 테스트는 "상태 코드(HTTP)"와 "에러 코드(COMMON-001 등)"가 설계표와 맞는지를 봅니다.
 */
class GlobalExceptionHandlerTest {

    // 테스트 대상 객체. 테스트마다 새로 만들지 않아도 되도록 필드로 둡니다.
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 처리기 1: BusinessException(SAMPLE_NOT_FOUND) → 404, SAMPLE-001.
     * 예외 안의 ErrorCode 값이 그대로 응답에 옮겨지는지도 함께 확인합니다.
     */
    @Test
    void businessExceptionBecomesErrorResponseWithItsStatusAndCode() {
        BusinessException exception = new BusinessException(ErrorCode.SAMPLE_NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(exception);

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "SAMPLE-001", "샘플을 찾을 수 없습니다");
    }

    /**
     * 처리기 2: MethodArgumentNotValidException(@Valid 검증 실패) → 400, COMMON-001.
     * 검증 실패 상세는 응답에 넣지 않고 공통 입력 오류로 통일합니다.
     */
    @Test
    void validationExceptionBecomesBadRequest() throws Exception {
        // MethodArgumentNotValidException 은 "어느 메서드의 인자가, 어떤 검사 결과로 틀렸는지"를 담습니다.
        // 여기서는 아무 메서드(toString)를 빌려 와서 최소한의 객체만 만듭니다. 처리기는 내용을 읽지 않습니다.
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("toString"), -1);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                parameter, new BeanPropertyBindingResult(new Object(), "request"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다");
    }

    /**
     * 처리기 3: HttpMessageNotReadableException(JSON 형식 오류) → 400, COMMON-001.
     */
    @Test
    void unreadableBodyBecomesBadRequest() {
        // Spring 7(Boot 4)에서는 (메시지, HttpInputMessage) 생성자를 씁니다. 여기서는 입력 메시지가 필요 없으므로 null을 넘깁니다.
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("잘못된 JSON", (HttpInputMessage) null);

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotReadableException(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다");
    }

    /**
     * 처리기 4: MethodArgumentTypeMismatchException(주소 값의 타입 불일치, 예: id 자리에 "abc") → 400, COMMON-001.
     */
    @Test
    void typeMismatchBecomesBadRequest() throws Exception {
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("toString"), -1);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", parameter, null);

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatchException(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다");
    }

    /**
     * 처리기 5: HttpMediaTypeNotSupportedException(JSON이 아닌 Content-Type) → 415, COMMON-003.
     */
    @Test
    void unsupportedMediaTypeBecomes415() {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException("text/plain 은 지원하지 않습니다");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMediaTypeNotSupported(exception);

        assertErrorResponse(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON-003", "지원하지 않는 Content-Type 입니다");
    }

    /**
     * 처리기 6: HttpRequestMethodNotSupportedException(지원하지 않는 HTTP 메서드) → 405, COMMON-002.
     */
    @Test
    void unsupportedMethodBecomes405() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodNotSupported(exception);

        assertErrorResponse(response, HttpStatus.METHOD_NOT_ALLOWED, "COMMON-002", "지원하지 않는 HTTP 메서드입니다");
    }

    /**
     * 처리기 7: NoResourceFoundException(없는 주소) → 404, COMMON-004.
     * 샘플이 없는 경우(SAMPLE-001)와 코드가 달라야 한다는 점도 여기서 확인합니다.
     */
    @Test
    void unknownPathBecomes404WithCommonCode() {
        // 생성자 인자: (HTTP 메서드, 메시지 조합용 값, 요청 주소). 주소 자리에 같은 값을 넣어 둡니다.
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/api/unknown", "/api/unknown");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResourceFound(exception);

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "COMMON-004", "요청한 경로를 찾을 수 없습니다");
    }

    /**
     * 처리기 8: 그 밖의 모든 예외(Exception) → 500, COMMON-500.
     * 응답 메시지에는 내부 예외 내용("boom")이 나오지 않아야 합니다. 자세한 내용은 서버 로그에만 남깁니다.
     */
    @Test
    void unexpectedExceptionBecomes500WithoutInternalDetails() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpectedException(new RuntimeException("boom"));

        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다");
        assertThat(response.getBody().error().message()).doesNotContain("boom");
    }

    /**
     * 8개 처리기가 공통으로 지켜야 하는 형태를 한곳에서 확인합니다.
     * 실패 응답은 success=false, data=null, error 에 code/message 가 채워져야 합니다.
     */
    private void assertErrorResponse(ResponseEntity<ApiResponse<Void>> response,
                                     HttpStatus expectedStatus,
                                     String expectedCode,
                                     String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().error()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(expectedCode);
        assertThat(response.getBody().error().message()).isEqualTo(expectedMessage);
    }
}
