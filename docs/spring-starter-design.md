# Spring Boot 4 스타터 프로젝트 설계

> 작성: 아키텍트(파인 1) / 대상: 팀장 승인 후 개발자(파인 4) 구현
> 목적: 이 저장소만 보고 처음 해 보는 사람도 순서대로 따라 하면 Spring Boot 4 프로젝트를 혼자 만들 수 있는 "껍데기(starter)"를 제공한다.

## 1. 기술 선택

| 항목 | 선택 | 이유 |
|---|---|---|
| 언어 | **Java 25 (Amazon Corretto 25)** | 사용자 요청(팀장 정정). Spring Boot 4 최소 요구는 Java 17 이상이며, 빌드 설정은 Gradle toolchain으로 25를 지정한다 |
| 프레임워크 | Spring Boot **4.0.x** (구현 시점 최신 패치로 고정) | 요청 사항. 정확한 최신 패치는 spring.io에서 확인 후 `build.gradle`에 기록한다 |
| 빌드 | Gradle (Groovy DSL) | 참고 자료(defect-lab)와 같은 도구. 초보자 자료가 가장 많다 |
| DB | H2 (메모리) | 설치 없이 바로 실행된다. 나중에 MySQL로 바꾸는 방법을 주석과 steps 문서에 적는다 |
| 롬복 | **사용하지 않음** | 초보자가 자동 생성 코드를 눈으로 볼 수 있도록 `record`와 직접 작성한 생성자를 쓴다 |
| 테스트 | JUnit 5 + Spring Boot Test + MockMvc | Spring Boot 4 기본 조합 |

### Spring Boot 4에서 바뀐 점 (3.x 자료와 다른 부분)
- 웹 스타터 이름: `spring-boot-starter-web` → **`spring-boot-starter-webmvc`**
- MockMvc 테스트 스타터: **`spring-boot-starter-webmvc-test`** 를 추가해야 `@WebMvcTest`를 쓸 수 있다
- `@SpringBootTest`는 MockMvc를 자동으로 만들지 않는다 → 필요하면 **`@AutoConfigureMockMvc`** 를 직접 붙인다
- 테스트 애노테이션 패키지: `org.springframework.boot.webmvc.test.autoconfigure.*` (`@WebMvcTest`, `@AutoConfigureMockMvc`)
- Jackson 3: 그룹 ID `tools.jackson`, 클래스 패키지 `tools.jackson.databind.*`. 단 애노테이션(`@JsonProperty` 등)은 기존 `com.fasterxml.jackson.annotation` 그대로
- `@MockBean` / `@SpyBean` 대신 Spring Framework의 `@MockitoBean` / `@MockitoSpyBean`
- 참고: 위 내용은 검색으로 확인했지만 공식 문서(Spring Boot 4.0 Migration Guide)를 직접 열어 보지는 못했다. 개발자는 컴파일 전에 공식 문서로 패키지 경로를 한 번 더 확인한다.

## 2. 디렉터리 구조

코드는 `src/main/java`, 테스트는 `src/test/java`에 둔다. 두 곳 모두 Gradle 기본 위치이므로 별도 설정이 필요 없다.
(이전 설계는 팀 규칙에 따라 테스트를 `tests/java`에 두고 `sourceSets`로 지정했으나, 팀장 결정으로 Gradle 표준 위치로 옮겼다.)

```
~/project/                         (기존 Python 할 일 앱과 같은 폴더에 함께 둔다. 기존 파일은 건드리지 않음)
├── build.gradle                   [신규] 빌드 설정 (의존성, 테스트 폴더 지정)
├── settings.gradle                [신규] 프로젝트 이름
├── .gitignore                     [신규] build/, .gradle/, .idea/ 등 제외
├── docs/
│   ├── spring-starter-design.md   [신규] 이 문서
│   └── spring-starter-steps.md    [신규] 단계별 따라 하기 안내서 (STEP 1~N, 각 단계의 파일 링크)
├── src/main/java/com/example/starter/
│   ├── StarterApplication.java    [신규] main 메서드, @SpringBootApplication
│   ├── common/
│   │   ├── ApiResponse.java       [신규] 모든 응답의 공통 형태 record
│   │   ├── ErrorCode.java         [신규] 에러 코드 enum (코드, HTTP 상태, 메시지)
│   │   ├── BusinessException.java  [신규] 업무 규칙 위반을 나타내는 예외
│   │   └── GlobalExceptionHandler.java [신규] @RestControllerAdvice, 예외 → ApiResponse 변환
│   ├── config/
│   │   └── AppProperties.java     [신규] application.yml의 app.* 값을 묶는 record (@ConfigurationProperties)
│   └── sample/
│       ├── SampleController.java  [신규] REST API (/api/samples)
│       ├── SampleService.java     [신규] 업무 로직, 트랜잭션
│       ├── SampleRepository.java  [신규] Spring Data JPA 인터페이스
│       ├── SampleEntity.java      [신규] JPA 엔터티 (테이블 samples)
│       ├── SampleRequest.java     [신규] 요청 본문 record (@NotBlank 검증)
│       └── SampleResponse.java    [신규] 응답 본문 record
├── src/main/resources/
│   └── application.yml            [신규] H2, JPA, 앱 설정
├── src/test/java/com/example/starter/
│   ├── StarterApplicationTests.java     [신규] 스프링 컨텍스트가 뜨는지 확인
│   ├── common/GlobalExceptionHandlerTest.java  [신규] 예외 → 응답 변환 확인 (@WebMvcTest 대신 단위 테스트)
│   ├── sample/SampleServiceTest.java    [신규] 서비스 단위 테스트 (Mockito)
│   ├── sample/SampleRepositoryTest.java [신규] @DataJpaTest로 DB 저장/조회
│   └── sample/SampleControllerTest.java [신규] @WebMvcTest + MockMvc로 HTTP 요청/응답/검증 실패
└── src/test/resources/
    └── application-test.yml       [신규] (선택) 테스트 전용 설정. 필요할 때만 만든다
```

패키지 이름 `com.example.starter`는 자리표시자다. 실제 프로젝트에서 바꾸기 쉽도록 steps 문서에 "패키지 이름 바꾸는 방법"을 한 단계로 넣는다.

## 3. 샘플 API (구현 범위)

| 메서드 | 경로 | 설명 | 성공 | 실패 |
|---|---|---|---|---|
| POST | /api/samples | 샘플 생성. body: `{"name": "..."}` | 201, `ApiResponse<SampleResponse>` | name 비어 있으면 400 (`COMMON-001`), 100자 초과면 400 (`COMMON-001`) |

**name 검증 기준 (아키텍트 결정, 리뷰 낮음-1):**
- 빈 값 검사: `@NotBlank` → `null`, `""`, 공백만 있는 값은 거절.
- 길이 검사: `@Size(max = 100)`. 글자 수는 자바 `String.length()`로 셉니다. 한글·영문·숫자는 1자이며, 일부 이모지는 2로 셉니다.
- DB 컬럼 `VARCHAR(100)`은 코드 포인트 기준이라 검증 기준보다 같거나 느슨합니다. 따라서 검증을 통과한 값은 DB에서 길이 오류가 날 수 없습니다.
- 검증 메시지: `이름은 100자 이하여야 합니다`.
- 테스트: 100자 → 201, 101자 → 400을 모두 확인합니다.
| GET | /api/samples/{id} | 단건 조회 | 200 | 없으면 404 (`SAMPLE-001`) |
| GET | /api/samples | 전체 목록 | 200 | - |

`ApiResponse` 형태 (성공/실패 모두 같은 틀):
```json
{ "success": true,  "data": { "id": 1, "name": "첫 샘플" }, "error": null }
{ "success": false, "data": null, "error": { "code": "SAMPLE-001", "message": "샘플을 찾을 수 없습니다" } }
```

## 4. 계층 규칙 (초보자 설명용)
- Controller: HTTP 요청을 받고 응답을 돌려준다. 업무 로직은 쓰지 않는다.
- Service: 업무 로직과 트랜잭션. Repository를 호출한다.
- Repository: DB 접근만 한다.
- Entity: DB 테이블과 1:1로 대응. 응답에는 직접 노출하지 않고 Response로 변환한다.
- 의존 방향은 Controller → Service → Repository 한 방향으로만 흐른다. 생성자 주입을 쓴다(`@Autowired` 필드 주입 금지).
- 예외: **테스트 클래스는 `@Autowired` 필드 주입을 허용한다.** 테스트 프레임워크 관례이며, 운영 코드(`src/main`)에는 적용되지 않는다. (아키텍트 결정, 리뷰 M-2)

## 5. 설정 (`application.yml`)
- `spring.datasource`: H2 메모리 DB (`jdbc:h2:mem:starter`)
- `spring.jpa.hibernate.ddl-auto: create-drop` (실습용)
- `spring.jpa.open-in-view: false` (초보자가 지연 로딩 문제를 덜 만나도록 명시)
- `app.name: starter` 등 `AppProperties`에 바인딩되는 값 1~2개
- 서버 포트 기본값(8080) 유지

## 6. 역할별 작업 범위

| 담당 | 작업 | 산출물 |
|---|---|---|
| 아키텍트(나) | 이 설계 문서, 작업 분배, 최종 취합 | `docs/spring-starter-design.md` |
| 개발자(파인 4) | 빌드 파일, `src/main/java`, `src/test/java`, `docs/spring-starter-steps.md` 구현. **모든 주석은 한국어 초보자용**, 참고 자료 주석 수준 | 위 디렉터리 구조 전체 |
| 리뷰어(파인 5) | 정확성, 주석 품질, Spring Boot 4 패키지 경로, 테스트 실행 결과 검토 | `docs/spring-starter-review.md` (기존 `docs/review.md`는 Python 프로젝트 것이라 건드리지 않음) |
| 리서처(파인 2) | 필요 시 공식 문서 패키지 경로 확인 요청이 오면 대응 | - |
| 디자이너(파인 3) | 이번 범위 없음 (API 응답 형태만 1절에 정의) | - |

## 7. 주석 기준
- 참고 자료(defect-lab 주석 버전)와 같은 수준: 클래스/메서드마다 "누가 호출하는지", "왜 필요한지"를 한국어로 적는다.
- 애노테이션(`@RestController`, `@Transactional` 등)은 처음 나오는 곳에서 한 줄 설명을 붙인다.
- 주석은 "무엇을"보다 "왜"를 적는다. 코드를 그대로 번역한 주석은 쓰지 않는다.

## 8. 실행 방법 (steps 문서에 들어갈 내용)
```bash
./gradlew bootRun          # 실행 (http://localhost:8080)
./gradlew test             # 테스트 실행
```
Gradle 래퍼는 **Gradle 9.9.0**으로 만든다. Gradle 8.14는 JDK 24까지만 공식 지원하므로 JDK 25에서는 쓰지 않는다. 9.9.0은 Corretto 25.0.5에서 빌드·테스트 통과를 확인했다 (아래 9절 참고).
`gradle wrapper --gradle-version 9.9.0`

### build.gradle toolchain 설정 (필수)
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```
`sourceCompatibility = JavaVersion.VERSION_21` 같은 옛 방식은 쓰지 않는다.

## 9. 현재 막힌 점 (반드시 확인)
- **이 환경에는 `java`와 `gradle`이 설치되어 있지 않다.** (`java -version`: command not found, `~/.gradle`·`~/.m2` 없음)
- 팀 규칙상 설치(환경 변경)는 개발자가 직접 하지 않는다. 따라서 **이번 구현은 컴파일·테스트를 실행하지 못한 상태로 진행**하고, 개발자는 코드를 읽어서 검토한다.
- **승인 요청 사항 (사용자 승인 전까지 설치하지 않는다):**
  1. Amazon Corretto 25 (JDK) 설치
  2. Gradle 설치 (또는 Gradle 래퍼 생성용)
  - 대안: 사용자 PC에서 `./gradlew test`를 직접 실행해 결과를 전달받는다.
- 승인 전까지 "테스트 통과"라고 보고하지 않는다.

## 10. 기존 파일과의 관계
- `src/todo/`, `tests/test_*.py`, `pyproject.toml`, `docs/design.md`, `docs/research.md`, `docs/ui.md`, `docs/review.md`는 기존 Python 할 일 앱의 것이다. **수정하거나 삭제하지 않는다.**
- 이 Spring 프로젝트는 같은 폴더에 공존한다. 충돌을 피하기 위해 Spring 전용 문서는 `spring-starter-*.md` 이름을 쓴다.
- Python 쪽 `pytest`는 `tests/*.py`만 수집하므로 `src/test/java/`와 간섭하지 않는다.

## 11. 완료 기준 (리뷰 통과 조건)
1. 빌드 파일, 패키지 구조, 공통 설정, 예외/응답 공통 처리, 샘플 Controller·Service·Repository·테스트가 모두 존재한다.
2. 모든 주석이 한국어이고, 클래스·메서드 단위 "호출처와 이유" 설명이 있다.
3. `docs/spring-starter-steps.md`만 보고 STEP 순서대로 따라 하면 같은 결과가 나온다.
4. Amazon Corretto 25 + Gradle 환경에서 `./gradlew test`가 통과한다. (환경 승인 전에는 미확인으로 보고한다)
