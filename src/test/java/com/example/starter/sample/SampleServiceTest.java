package com.example.starter.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.starter.common.BusinessException;
import com.example.starter.common.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SampleService 의 업무 로직을 확인하는 "단위 테스트"입니다.
 *
 * 핵심 아이디어: DB(Repository)를 "가짜(Mock)"로 바꿔서, 서비스 코드만 따로 검사합니다.
 *   - 실제 DB가 없어도 동작을 확인할 수 있고, 실행이 빠릅니다.
 *
 * 누가 실행하나요?
 *   - ./gradlew test 가 자동으로 실행합니다.
 */
// MockitoExtension: @Mock 으로 표시된 가짜 객체를 테스트 시작 전에 만들어 주는 확장 기능입니다.
@ExtendWith(MockitoExtension.class)
class SampleServiceTest {

    // @Mock: 진짜 SampleRepository 대신 "가짜"를 만듭니다. 가짜가 무엇을 돌려줄지는 given(...)으로 정합니다.
    @Mock
    private SampleRepository sampleRepository;

    // @InjectMocks: 테스트 대상(SampleService)을 만들면서, 위의 가짜 Repository를 생성자에 넣어 줍니다.
    @InjectMocks
    private SampleService sampleService;

    /**
     * create 를 호출하면 Repository 의 save 가 호출되고, 저장된 결과가 응답으로 돌아오는지 확인합니다.
     */
    @Test
    void createSavesAndReturnsResponse() {
        // 가짜 Repository 에게 "save 가 불리면 이 엔터티를 돌려줘"라고 약속합니다.
        // 주의: 여기서는 id 가 없는 엔터티입니다(실제 id 발급은 DB가 하므로 SampleRepositoryTest에서 확인합니다).
        SampleEntity stored = new SampleEntity("첫 샘플");
        given(sampleRepository.save(any(SampleEntity.class))).willReturn(stored);

        SampleResponse response = sampleService.create("첫 샘플");

        assertThat(response.name()).isEqualTo("첫 샘플");
    }

    /**
     * 존재하는 id 로 조회하면 해당 샘플이 돌아오는지 확인합니다.
     */
    @Test
    void findByIdReturnsSampleWhenExists() {
        // 실제 DB 없이 Entity 를 직접 만들어 가짜 Repository 가 돌려주게 합니다.
        SampleEntity stored = new SampleEntity("조회용");
        given(sampleRepository.findById(1L)).willReturn(Optional.of(stored));

        SampleResponse response = sampleService.findById(1L);

        assertThat(response.name()).isEqualTo("조회용");
    }

    /**
     * 없는 id 로 조회하면 SAMPLE_NOT_FOUND 예외가 던져지는지 확인합니다.
     * 이 예외가 GlobalExceptionHandler 를 거쳐 404가 됩니다.
     */
    @Test
    void findByIdThrowsWhenMissing() {
        given(sampleRepository.findById(99L)).willReturn(Optional.empty());

        // assertThatExceptionOfType: 던져진 예외의 타입을 확인하고, 그 예외 안의 ErrorCode 를 꺼내 비교합니다.
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> sampleService.findById(99L))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.SAMPLE_NOT_FOUND);
    }

    /**
     * 전체 목록 조회가 Repository 의 결과를 모두 응답 목록으로 옮기는지 확인합니다.
     */
    @Test
    void findAllReturnsAllSamples() {
        given(sampleRepository.findAll()).willReturn(List.of(
                new SampleEntity("A"),
                new SampleEntity("B")));

        List<SampleResponse> responses = sampleService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("A");
        assertThat(responses.get(1).name()).isEqualTo("B");
    }
}
