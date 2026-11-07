package com.example.starter.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 할 일 수정 요청의 본문(body)입니다. PUT 은 전체 교체 방식이라 제목과 완료 여부를 모두 보내야 합니다.
 *
 * 요청 예시: {"title": "우유 두 개 사기", "completed": true}
 *
 * 누가 사용하나요?
 *   - TodoController.update 의 @RequestBody 인자로 자동 변환됩니다.
 *
 * 왜 completed 를 Boolean(상자형)으로 쓰나요?
 *   - 기본형 boolean 은 값을 생략하면 자동으로 false가 됩니다. 생략과 false를 구별하려면
 *     null 이 가능한 Boolean 을 써야 @NotNull 검사가 "생략"을 잡아낼 수 있습니다.
 *   - 검증을 통과하면 null 이 아니므로, Service 에서 boolean 으로 풀어 써도 안전합니다.
 */
// 제목 검사는 생성 요청(TodoRequest)과 같은 기준입니다. 규칙이 한곳에서 바뀌도록 나중에 공통화할 수 있습니다.
// @NotNull: 값이 아예 없으면(키 자체가 빠지면) 검증 실패가 됩니다. 검증 실패 시 400(COMMON-001) 응답입니다.
public record TodoUpdateRequest(
        @NotBlank(message = "제목은 비어 있을 수 없습니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        String title,

        @NotNull(message = "완료 여부는 필수입니다")
        Boolean completed
) {
}
