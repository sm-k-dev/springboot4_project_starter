package com.example.starter.sample;

import com.example.starter.common.BusinessException;
import com.example.starter.common.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 샘플에 관한 업무 로직을 담당합니다. (Service 계층)
 *
 * 누가 호출하나요?
 *   - SampleController 가 호출합니다.
 *
 * 무엇을 호출하나요?
 *   - SampleRepository 로 DB를 다룹니다.
 *
 * 왜 Controller에 로직을 쓰지 않나요?
 *   - 업무 규칙을 Service에 모으면, 웹(HTTP)이 아닌 곳에서 같은 규칙을 다시 쓸 수 있고
 *     테스트도 쉬워집니다. (SampleServiceTest 참고)
 */
// @Service: "업무 로직을 담당하는 Bean"이라는 표시입니다. Spring이 자동으로 만들어 관리합니다.
@Service
public class SampleService {

    /** DB 접근 도구. 생성자로 받습니다(생성자 주입). */
    private final SampleRepository sampleRepository;

    /**
     * 생성자 주입: Spring이 SampleRepository 객체를 만들어서 여기에 넣어 줍니다.
     * 필드에 @Autowired 를 붙이는 방식보다 테스트할 때 가짜(mock) 객체를 넣기 쉽습니다.
     */
    public SampleService(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    /**
     * 새 샘플을 저장하고 결과를 돌려줍니다.
     * SampleController.create 에서 호출합니다.
     */
    // @Transactional: 이 메서드 안의 DB 작업을 "한 묶음"으로 묶습니다.
    // 중간에 실패하면 모두 취소(롤백)되어 데이터가 반쯤만 저장되는 일을 막습니다.
    @Transactional
    public SampleResponse create(String name) {
        SampleEntity saved = sampleRepository.save(new SampleEntity(name));
        return SampleResponse.from(saved);
    }

    /**
     * id로 샘플 한 개를 찾습니다. 없으면 SAMPLE_NOT_FOUND 예외를 던집니다.
     * SampleController.findOne 에서 호출합니다.
     */
    // readOnly = true: "이 메서드는 데이터를 바꾸지 않는다"고 알려 줍니다. 조회 성능을 조금 높여 줍니다.
    @Transactional(readOnly = true)
    public SampleResponse findById(Long id) {
        // Optional: "값이 있을 수도, 없을 수도 있음"을 표현하는 상자입니다. 없으면 orElseThrow 가 예외를 던집니다.
        SampleEntity entity = sampleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAMPLE_NOT_FOUND));
        return SampleResponse.from(entity);
    }

    /**
     * 저장된 샘플 전체 목록을 돌려줍니다. SampleController.findAll 에서 호출합니다.
     */
    @Transactional(readOnly = true)
    public List<SampleResponse> findAll() {
        // stream().map(...).toList(): 각 Entity를 Response로 바꿔서 새 목록을 만듭니다.
        return sampleRepository.findAll().stream()
                .map(SampleResponse::from)
                .toList();
    }
}
