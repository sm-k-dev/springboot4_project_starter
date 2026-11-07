package com.example.starter.common;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드를 한곳에 모아 둔 목록입니다.
 *
 * 누가 사용하나요?
 *   - Service가 업무 규칙 위반 시 BusinessException(ErrorCode.XXX)을 던지고,
 *   - GlobalExceptionHandler가 HTTP 상태 코드와 메시지를 꺼내 응답을 만듭니다.
 *
 * 왜 enum인가요?
 *   - 에러 코드 문자열을 여기저기 직접 쓰면 오타가 나기 쉽습니다.
 *     enum으로 묶어 두면 컴파일러가 오타를 잡아 줍니다.
 *
 * 새 에러를 추가하는 방법: 아래 목록에 한 줄 추가하면 됩니다. 코드는 "도메인-번호" 형식을 지킵니다.
 */
public enum ErrorCode {

    // 공통 입력 오류: 요청 본문이 잘못되었을 때 (검증 실패, JSON 형식 오류, 타입 불일치 등)
    INVALID_INPUT("COMMON-001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),

    // 공통 요청 오류: 지원하지 않는 HTTP 메서드로 요청했을 때 (예: 목록 주소에 DELETE)
    METHOD_NOT_ALLOWED("COMMON-002", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다"),

    // 공통 요청 오류: Content-Type이 JSON이 아닐 때 (예: text/plain 으로 POST)
    UNSUPPORTED_MEDIA_TYPE("COMMON-003", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다"),

    // 공통 경로 오류: 주소 자체가 없을 때 (예: /api/없는주소)
    NOT_FOUND("COMMON-004", HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다"),

    // 공통 서버 오류: 예상하지 못한 예외가 났을 때. 자세한 내용은 서버 로그에만 남기고 응답에는 노출하지 않습니다.
    INTERNAL_ERROR("COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),

    // 샘플 도메인 오류: 찾는 샘플이 없을 때
    SAMPLE_NOT_FOUND("SAMPLE-001", HttpStatus.NOT_FOUND, "샘플을 찾을 수 없습니다"),

    // 할 일 도메인 오류: 찾는 할 일이 없을 때 (조회·수정·삭제 공통)
    TODO_NOT_FOUND("TODO-001", HttpStatus.NOT_FOUND, "할 일을 찾을 수 없습니다");

    /** 응답의 error.code 에 들어갈 값 */
    private final String code;
    /** 응답의 HTTP 상태 코드 (400, 404 등) */
    private final HttpStatus status;
    /** 응답의 error.message 에 들어갈 설명 */
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    /**
     * 에러 코드 문자열(예: "SAMPLE-001")을 돌려줍니다.
     * 호출처: ApiResponse.fail 이 응답의 error.code 칸에 넣을 때 씁니다.
     */
    public String getCode() {
        return code;
    }

    /**
     * HTTP 상태 코드(예: 404)를 돌려줍니다.
     * 호출처: GlobalExceptionHandler 가 ResponseEntity 의 상태를 정할 때 씁니다.
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 사람이 읽을 수 있는 설명 문장을 돌려줍니다.
     * 호출처: ApiResponse.fail 이 error.message 칸에 넣을 때 씁니다.
     */
    public String getMessage() {
        return message;
    }
}
