package com.example.starter.sample;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 샘플 생성 요청의 본문(body)입니다. 클라이언트가 보내는 JSON 을 이 모양으로 받습니다.
 *
 * 요청 예시: {"name": "첫 샘플"}
 *
 * 누가 사용하나요?
 *   - SampleController.create 의 @RequestBody 인자로 자동 변환됩니다.
 *
 * 왜 record 인가요?
 *   - 요청 값을 담기만 하는 용도라 record가 가장 짧고 명확합니다.
 */
// @NotBlank: 값이 null, 빈 문자열(""), 공백만 있는 문자열("  ") 이면 검증 실패가 됩니다.
// @Size(max = 100): 100자를 넘으면 검증 실패가 됩니다. DB 컬럼 길이(SampleEntity의 length = 100)와 맞춘 값입니다.
//   이 검사가 없으면 101자 이름이 DB 저장 단계에서 실패해 500 오류가 나므로, 요청 단계에서 미리 막습니다.
//   글자 수 기준: 자바 String.length() 로 셉니다. 한글·영문은 1자, 일부 이모지는 2로 셉니다(자바 내부 문자 단위 때문).
//   DB(H2 VARCHAR)는 코드 포인트(사람이 보는 글자) 기준이라 이모지가 섞이면 자바 쪽이 더 엄격하게 막습니다.
//   즉 자바 검증을 통과한 값은 항상 DB 길이 제한 안에 들어가므로 저장 단계 500은 나지 않습니다.
// 검증 실패 시 GlobalExceptionHandler 가 400 응답으로 바꿔 줍니다.
public record SampleRequest(
        @NotBlank(message = "이름은 비어 있을 수 없습니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        String name
) {
}
