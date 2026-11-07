package com.example.starter.common;

/**
 * "업무 규칙에 어긋난다"는 것을 알려 주기 위한 예외(에러 신호)입니다.
 * 예: 찾는 샘플이 없음 → ErrorCode.SAMPLE_NOT_FOUND
 *
 * 누가 던지나요?
 *   - SampleService 등 업무 로직을 담당하는 코드가 던집니다.
 *
 * 누가 잡나요?
 *   - GlobalExceptionHandler 가 잡아서 알맞은 HTTP 응답(404 등)으로 바꿉니다.
 *
 * 왜 RuntimeException 인가요?
 *   - 모든 메서드에 throws 를 달지 않아도 되도록 "검사하지 않는 예외"로 만듭니다.
 */
public class BusinessException extends RuntimeException {

    /** 어떤 에러인지 알려 주는 코드 묶음 */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        // 부모(RuntimeException)에게도 메시지를 넘겨서 로그에 설명이 찍히도록 합니다.
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
