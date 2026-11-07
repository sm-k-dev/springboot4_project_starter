package com.example.starter.sample;

import com.example.starter.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 샘플 API의 입구입니다. HTTP 요청을 받아 Service에 넘기고, 결과를 응답으로 돌려줍니다.
 *
 * 제공하는 API:
 *   POST /api/samples       샘플 생성 (201)
 *   GET  /api/samples/{id}  단건 조회 (200, 없으면 404)
 *   GET  /api/samples       전체 목록 (200)
 *
 * 규칙: Controller는 "받고 넘기고 돌려주기"만 합니다. 업무 판단은 SampleService 에 맡깁니다.
 */
// @RestController: 이 클래스의 메서드 반환값을 JSON으로 자동 변환해 응답하는 Controller라는 표시입니다.
@RestController
// @RequestMapping: 이 클래스의 모든 API 주소 앞에 공통으로 붙는 경로입니다.
@RequestMapping("/api/samples")
public class SampleController {

    private final SampleService sampleService;

    /** 생성자 주입: Spring이 SampleService 객체를 넣어 줍니다. */
    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    /**
     * POST /api/samples : 샘플을 만듭니다.
     * 요청 예시: {"name": "첫 샘플"}
     */
    // @PostMapping: HTTP POST 요청을 이 메서드로 연결합니다. (주소는 클래스의 /api/samples 그대로)
    @PostMapping
    public ResponseEntity<ApiResponse<SampleResponse>> create(
            // @RequestBody: 요청 본문 JSON을 SampleRequest 객체로 바꿔 줍니다.
            // @Valid: SampleRequest 의 @NotBlank 같은 검증을 실행합니다. 실패하면 GlobalExceptionHandler 가 400을 돌려줍니다.
            @Valid @RequestBody SampleRequest request) {
        SampleResponse created = sampleService.create(request.name());
        // 새 자원이 만들어졌으므로 HTTP 상태는 200이 아니라 201(Created)을 씁니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    /**
     * GET /api/samples/{id} : id 로 샘플 한 개를 조회합니다. 없으면 Service가 던진 예외가 404로 바뀝니다.
     */
    // "/{id}" 부분은 주소에 적힌 숫자를 아래 매개변수로 넘기라는 뜻입니다. 예: /api/samples/3 → id = 3
    // @PathVariable("id"): 변수 이름을 직접 적어 둡니다. 이름 추론에 기대면 컴파일 옵션에 따라 실패할 수 있어서입니다.
    @GetMapping("/{id}")
    public ApiResponse<SampleResponse> findOne(@PathVariable("id") Long id) {
        return ApiResponse.ok(sampleService.findById(id));
    }

    /**
     * GET /api/samples : 샘플 전체 목록을 조회합니다.
     */
    @GetMapping
    public ApiResponse<List<SampleResponse>> findAll() {
        return ApiResponse.ok(sampleService.findAll());
    }
}
