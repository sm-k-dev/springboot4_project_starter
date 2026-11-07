package com.example.starter.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 Controller에서 던져진 예외를 한곳에서 잡아 ApiResponse 모양으로 바꿔 주는 클래스입니다.
 *
 * 누가 호출하나요?
 *   - Spring이 Controller에서 예외가 발생하면 자동으로 이 클래스의 메서드를 찾아 호출합니다.
 *     (우리가 직접 호출하지 않습니다)
 *
 * 왜 필요한가요?
 *   - Controller마다 try-catch를 쓰면 코드가 지저분해집니다.
 *     한곳에서 처리하면 응답 모양이 항상 같아지고 관리가 쉬워집니다.
 *   - 설계 규칙: 성공이든 실패든 응답은 항상 ApiResponse 모양이어야 합니다.
 *     그래서 Spring 기본 오류 화면/JSON이 나가지 않도록 주요 예외를 모두 여기서 처리합니다.
 *
 * 메서드 선택 규칙: Spring은 "가장 구체적인" 예외 타입의 메서드를 고릅니다.
 *   마지막의 Exception.class 처리는 위에서 잡히지 않은 나머지를 모두 500으로 바꾸는 최후의 그물입니다.
 */
// @RestControllerAdvice: "모든 Controller를 감시하는 도우미"라는 표시입니다.
// 반환값을 JSON으로 자동 변환해 줍니다. (@ControllerAdvice + @ResponseBody 의 합)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 로그 기록 도구. 500 오류는 응답에는 숨기고 여기 로그에만 자세히 남깁니다.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 업무 규칙 위반(BusinessException)이 발생했을 때 호출됩니다.
     * 예외 안에 들어 있는 ErrorCode의 HTTP 상태(예: 404)와 메시지를 그대로 사용합니다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResponse.fail(errorCode));
    }

    /**
     * @Valid 검증(예: name이 비어 있음, 100자 초과)에 실패했을 때 호출됩니다.
     * 어떤 필드가 틀렸는지는 응답에 넣지 않고, 공통 입력 오류(COMMON-001)로 통일합니다.
     * (필드별 상세 메시지가 필요해지면 이 메서드를 확장하세요)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ErrorCode.INVALID_INPUT));
    }

    /**
     * 요청 본문이 JSON 형식으로 읽히지 않을 때 호출됩니다. (예: 따옴표 빠진 JSON, 빈 본문)
     * 이것도 클라이언트의 잘못이므로 400으로 응답합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ErrorCode.INVALID_INPUT));
    }

    /**
     * 주소의 값 타입이 맞지 않을 때 호출됩니다. (예: GET /api/samples/abc 처럼 id 자리에 글자)
     * 클라이언트의 잘못이므로 400으로 응답합니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ErrorCode.INVALID_INPUT));
    }

    /**
     * Content-Type 이 JSON이 아닐 때 호출됩니다. (예: text/plain 으로 POST)
     * 지원하지 않는 형식이므로 415로 응답합니다.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    /**
     * 주소는 있지만 HTTP 메서드가 맞지 않을 때 호출됩니다. (예: 목록 주소에 DELETE 요청)
     * 405로 응답합니다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 존재하지 않는 주소로 요청했을 때 호출됩니다. (예: /api/없는주소)
     * 404로 응답하되, 샘플이 없는 경우(SAMPLE-001)와 구분하기 위해 COMMON-004 코드를 씁니다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ErrorCode.NOT_FOUND));
    }

    /**
     * 위에서 잡지 못한 모든 예외의 최후 그물입니다. 예상하지 못한 버그가 여기 해당합니다.
     * 응답에는 내부 정보를 숨기고 500 + INTERNAL_ERROR 만 보냅니다.
     * 자세한 원인은 서버 로그에 남겨서 개발자가 확인할 수 있게 합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("예상하지 못한 오류가 발생했습니다", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }
}
