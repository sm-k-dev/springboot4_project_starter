package com.example.starter.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 할 일 생성 요청의 본문(body)입니다. 클라이언트가 보내는 JSON 을 이 모양으로 받습니다.
 *
 * 요청 예시: {"title": "우유 사기"}
 *
 * 누가 사용하나요?
 *   - TodoController.create 의 @RequestBody 인자로 자동 변환됩니다.
 *
 * 왜 completed 가 없나요?
 *   - 새 할 일은 항상 미완료로 시작합니다. 생성 때 완료 여부를 받으면 규칙이 흐려지므로 받지 않습니다.
 */
// @NotBlank: 값이 null, 빈 문자열(""), 공백만 있는 문자열("  ") 이면 검증 실패가 됩니다.
// @Size(max = 100): 100자를 넘으면 검증 실패가 됩니다. DB 컬럼 길이(TodoEntity의 length = 100)와 맞춘 값입니다.
//   글자 수 기준은 자바 String.length() 입니다. 이 검사가 없으면 DB 저장 단계에서 500이 나므로 요청 단계에서 미리 막습니다.
// 검증 실패 시 GlobalExceptionHandler 가 400(COMMON-001) 응답으로 바꿔 줍니다.
public record TodoRequest(
        @NotBlank(message = "제목은 비어 있을 수 없습니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        String title
) {
}
