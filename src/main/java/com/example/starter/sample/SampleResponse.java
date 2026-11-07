package com.example.starter.sample;

/**
 * 샘플 응답 데이터입니다. ApiResponse 의 data 칸에 들어갑니다.
 *
 * 응답 예시: {"id": 1, "name": "첫 샘플"}
 *
 * 누가 만드나요?
 *   - SampleService 가 SampleEntity 를 from(...) 으로 변환해 만듭니다.
 *
 * 왜 Entity를 그대로 주지 않나요?
 *   - 응답에 필요한 값만 골라서 주기 위해서입니다. DB 내부 구조가 밖으로 새지 않습니다.
 */
public record SampleResponse(Long id, String name) {

    /**
     * Entity → Response 변환 메서드입니다.
     * 변환 로직을 한곳에 모아 두면 나중에 필드가 바뀌어도 고칠 곳이 한 군데입니다.
     */
    public static SampleResponse from(SampleEntity entity) {
        return new SampleResponse(entity.getId(), entity.getName());
    }
}
