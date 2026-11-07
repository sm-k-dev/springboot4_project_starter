package com.example.starter.common;

/**
 * 모든 API 응답의 공통 모양입니다. 성공이든 실패든 같은 틀을 씁니다.
 *
 * 응답 예시 (성공):
 *   { "success": true, "data": {...}, "error": null }
 * 응답 예시 (실패):
 *   { "success": false, "data": null, "error": { "code": "SAMPLE-001", "message": "..." } }
 *
 * 누가 호출하나요?
 *   - Controller가 성공 시 ApiResponse.ok(...), GlobalExceptionHandler가 실패 시 ApiResponse.fail(...)을 호출합니다.
 *
 * 왜 필요한가요?
 *   - 프론트엔드(또는 다른 개발자)가 "항상 같은 모양"으로 응답을 읽을 수 있게 하기 위해서입니다.
 *
 * @param <T> 성공 시 담을 데이터의 타입
 */
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    /** 성공 응답을 만듭니다. 실패 정보(error)는 비어 있습니다(null). */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 실패 응답을 만듭니다. 데이터(data)는 비어 있습니다(null).
     * 에러 코드와 메시지는 ErrorCode 에 한 곳에 모여 있으므로 여기서는 그대로 옮기기만 합니다.
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * 실패 정보 한 묶음입니다. code는 "SAMPLE-001"처럼 기계가 읽는 값, message는 사람이 읽는 설명입니다.
     */
    public record ErrorBody(String code, String message) {
    }
}
