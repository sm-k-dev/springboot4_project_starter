# Spring Boot 스타터 정적 검토 결과

> 작성: 리뷰어(파인 5) / 요청: 아키텍트(파인 1)
> 기준 문서: `docs/spring-starter-design.md`
> 대상: `build.gradle`, `settings.gradle`, `.gitignore`, `src/main/**`, `src/test/java/**`, `docs/spring-starter-steps.md`
> 검토 방법: 코드 정적 읽기만 수행. JDK/Gradle이 없어 컴파일·테스트는 실행하지 않음 (요청에 따라 개발자가 실행 예정).

## 판정: **반려** (수정 필요 N건)

설계와 어긋나는 빌드 설정(Java 버전), 실제 요청에서 500이 나는 버그, 설계가 요구한 "모든 응답 같은 틀"을 깨는 경로가 있습니다. 아래 "수정 목록"을 반영한 뒤 다시 검토합니다.

---

## 0-5. 테스트 폴더 이동 재검토 — **통과**

| 항목 | 상태 | 확인 근거 |
|---|---|---|
| Java 테스트 5개 이동 | ✅ | `src/test/java/com/example/starter/` 아래 `StarterApplicationTests`, `common/GlobalExceptionHandlerTest`, `sample/` 3개 파일 존재 |
| `tests/java` 삭제 | ✅ | 해당 폴더 없음. Python `tests/*.py` 4개는 그대로 |
| `build.gradle` sourceSets 제거 | ✅ | `sourceSets` 블록 없음. 55행 주석만 남음 (기본 위치 설명) |
| 설계·steps 문서 표기 수정 | ✅ | `tests/java` 잔존 표기 없음. 설계 28~29행 변경 이력 설명은 의도된 기록 |
| 패키지 경로 | ✅ | 테스트 파일 패키지 선언이 `com.example.starter...`로 그대로라 Gradle 기본 위치와 일치 |

**테스트 개수:** 27개 (아키텍트 재실행 기준, 리뷰어 미실행). 이동 전과 같습니다.

**참고(차단 아님):** 공동 규칙(`CLAUDE.md` 3항)은 "테스트는 `tests/`"로 적혀 있습니다. Java 테스트가 `src/test/java`로 가면서 이 규칙과 어긋납니다. 팀장 결정이므로 리뷰는 통과로 두되, `CLAUDE.md`에 "스프링 스타터는 Gradle 표준 위치 예외"를 적을지 팀장이 정해 주세요.

### 판정

**통과.** 차단 사유 없음.

## 0-4. 낮은 우선순위 3건 반영 재검토 — **통과 (최종)**

| 항목 | 상태 | 확인 근거 |
|---|---|---|
| `@Size` 글자 수 기준 | ✅ 반영 | `SampleRequest.java:20-22` 기준 설명 추가. 설계 3절 `:77-80`, steps `:82`에도 같은 기준 기술 |
| 100자 201 테스트 | ✅ 반영 | `SampleControllerTest.java:99-100` `createWithExactly100CharsReturns201` (101자 400 테스트와 짝) |
| 처리기 단위 테스트 8개 | ✅ 반영 | `GlobalExceptionHandlerTest`: 비즈니스 예외, 검증, JSON 오류, 타입 불일치, 415, 405, 404, 500 (8개 처리기 전부) |
| steps 규칙 1줄 | ✅ 반영 | `steps:82` 1~100자, 공백만 불가, 글자 수 기준, 위반 시 400 COMMON-001 |
| dependency-management 1.1.7 | ✅ 유지 | 플러그인 포털 최신 릴리스 확인 결과 유지(아키텍트 보고 기준 인정) |

**테스트 개수 확인:** 27개 = StarterApplicationTests 2 + GlobalExceptionHandlerTest 8 + SampleServiceTest 4 + SampleRepositoryTest 2 + SampleControllerTest 11. 아키텍트 재실행 결과(27개 통과)와 일치합니다. 리뷰어는 직접 실행하지 않았습니다.

**길이 기준 정합성:** 자바 `String.length()`는 UTF-16 단위입니다. 보조 문자(이모지)는 2단위로 세므로 Java 검증을 통과한 값은 코드 포인트 수가 100 이하입니다. 따라서 DB `VARCHAR(100)`을 넘을 수 없습니다. 설계와 주석의 설명이 맞습니다.

**bootRun·STEP 4 curl 5건 일치:** 아키텍트 보고 기준 인정합니다.

### 최종 판정

**통과.** 차단 사유 없음, 남은 미결 사항 없음. 개발 단계의 리뷰는 종료합니다.

## 0-3. 최종 재검토 (개발자 수정 완료 후) — **통과**

테스트 20개 개수는 파일 기준으로 맞습니다(StarterApplicationTests 2, GlobalExceptionHandlerTest 2, SampleServiceTest 4, SampleRepositoryTest 2, SampleControllerTest 10). 리뷰어는 `./gradlew test`를 직접 실행하지 않았고, 통과 결과는 아키텍트 재실행을 근거로 합니다.

### 수정 목록 체크 (7건 + 결정 사항)

| 항목 | 상태 | 확인 근거 |
|---|---|---|
| H-1 Java 25 / Gradle 9.9.0 | ✅ 반영 | `build.gradle:25`, `gradle-wrapper.properties` 9.9.0, steps 12~34행 25·9.9.0 일치, 설계 118~119행 일치 |
| H-2 이름 100자 초과 500 | ✅ 반영 | `SampleRequest.java:23` `@Size(max = 100)`, `SampleControllerTest` 101자 → 400 케이스 |
| H-3 응답 틀 통일 | ✅ 반영 | `GlobalExceptionHandler.java`에 400(타입 불일치)·415·405·404(경로)·500 최후 그물 추가, `ErrorCode`에 COMMON-002~004, COMMON-500 추가. 500은 메시지를 숨기고 로그에만 남김(`SampleControllerTest` "boom" 미노출 검증) |
| M-1 테스트 주석·검증 | ✅ 반영 | `verify(never())` 추가, `SampleServiceTest` id 주석 정정, `SampleRepositoryTest`에 `flush()`/`clear()` 추가 |
| M-2 테스트 필드 주입 | ✅ 반영 | 설계 4절에 테스트 예외 명시(설계 `:89`), 운영 코드 `src/main`은 생성자 주입 유지 |
| M-3 4.0.9 주석 | ✅ 반영 | `build.gradle:13` "Maven Central 존재 확인(2026-11-07)" |
| M-4 패키지 경로 | ✅ 인정 | 테스트 컴파일 통과 보고 기준. `SampleRepositoryTest:8` `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| L-2 DB 비밀번호 | ✅ 반영 | steps 173~184행 `${DB_PASSWORD}`, `application.yml:13,16` 실습용 표기 |
| `@PathVariable` 명시 (L-2 항목) | ✅ 반영 | `SampleController.java:59` `@PathVariable("id")` |

### 신규 확인 사항 (차단 아님)

- **`@Size` 글자 수 기준 (낮음):** Java `String.length()`는 UTF-16 단위입니다. 한글은 1자이므로 문제없고, 이모지 같은 보조 문자는 2단위로 세져 DB 길이 기준과 어긋날 수 있습니다. 필요하면 나중에 조정하세요.
- **`dependency-management` 플러그인 (낮음):** 추가해도 동작에 문제는 없습니다. 플러그인 버전 1.1.7은 리뷰어가 확인하지 못했습니다.
- **`GlobalExceptionHandlerTest` (낮음):** 새 핸들러는 `SampleControllerTest`의 HTTP 테스트로 확인되므로 단위 테스트 추가는 필수가 아닙니다.

### 판정

**통과.** 반려 사유였던 항목은 모두 파일에서 확인되었습니다. 남은 것은 위 낮음 항목 3건뿐이며 차단 사유가 아닙니다.

## 0-2. 2차 재검토 (결정 반영 후) — 보류 (이력)

아키텍트 결정(Java 25 유지, Gradle 9.9.0, 테스트 필드 주입 허용, 4.0.9 존재 확인, 패키지 경로 확인)은 반영 여부를 파일로 확인했습니다.

| 항목 | 현재 상태 (파일 확인) | 판정 |
|---|---|---|
| Java 25 toolchain | `build.gradle:25` 25 | 반영됨 |
| Gradle 9.9.0 | `gradle/wrapper/gradle-wrapper.properties` 9.9.0 | 반영됨 (JDK 25 검증은 아키텍트 보고에 의존, 리뷰어 미확인) |
| 4.0.9 존재 | `build.gradle:14` 4.0.9 | 존재는 아키텍트 보고에 의존 |
| M-3 주석 | `build.gradle:13` 여전히 "공식 확인 필요" 문구 | **미반영** — 주석을 "Maven Central 존재 확인" 등으로 고쳐야 함 |
| 테스트 필드 주입 허용 | 설계 4절 명시 여부 확인 안 함 (설계 파일 미수정 확인 필요) | 설계 문서 반영 확인 필요 |
| 패키지 경로 (M-4) | 테스트 컴파일 통과 보고에 의존. 리뷰어는 컴파일 로그를 보지 못함 | 아키텍트 보고 기준 인정 |
| H-1 steps 문서 JDK 21 표기 | `docs/spring-starter-steps.md` 수정 시각 14:47, 내용 미변경 (12, 15, 45, 140행 21) | **미반영** |
| H-2 이름 100자 초과 500 | `SampleRequest.java` 수정 시각 14:45, `@Size` 없음 | **미반영** |
| H-3 응답 틀 통일 | `GlobalExceptionHandler.java` 수정 시각 14:45, 핸들러 3종 그대로 | **미반영** |
| M-1 테스트 주석·검증 | `SampleControllerTest`, `SampleServiceTest`, `SampleRepositoryTest` 주석 미확인, 수정 여부 미확인 | **미확인** (재검토 시 읽기 필요) |

결론: 소스·steps 수정이 아직 파일에 반영되지 않았습니다. 개발자 수정 완료 후 다시 검토합니다. 현 시점 판정은 **반려 유지**입니다.

## 0. 아키텍트 수정 반영 후 재확인 (최신 파일 기준)

| 항목 | 결과 | 비고 |
|---|---|---|
| `build.gradle` toolchain 25 | **해결** | 설계 1절·8절과 일치. 아래 H-1의 나머지(문서)는 미해결 |
| `build.gradle` dependency-management 1.1.7 추가 | 확인 | 문제 없음. Boot 플러그인이 BOM을 이미 관리하므로 필수는 아님. 플러그인 버전 1.1.7은 리뷰어가 확인하지 못함 |
| `build.gradle:13` 버전 주석 "공식 확인 필요" | **미해결** | M-3 그대로. 주석 문구는 "확인된 값"처럼 읽혀 수정 필요 |
| `GlobalExceptionHandlerTest` 생성자 변경 `(String, HttpInputMessage)` | 확인 | Spring 6.1+/7에서 쓰는 생성자 형태와 일치. `null` 캐스팅도 모호성 없음 |
| 테스트 14개 통과 (아키텍트 보고) | **리뷰어 미확인** | 리뷰어는 실행하지 않음. 14 = StarterApplicationTests 2 + GlobalExceptionHandlerTest 2 + SampleServiceTest 4 + SampleRepositoryTest 2 + SampleControllerTest 4 |
| steps 문서의 JDK 21 표기 | **미해결 (H-1 잔여)** | `docs/spring-starter-steps.md:12, 15, 45, 140` 이 아직 21. build.gradle은 25 → 안내 문서와 빌드가 불일치. 함께 고쳐야 함 |
| steps STEP 1 `gradle wrapper --gradle-version 8.14` | **위험 잔존** | JDK 25 런타임에서는 Gradle 9.1 이상이 필요할 수 있음 (H-1 참고). 개발자 실행 결과로 확인 필요 |

나머지 수정 목록(H-2 이름 길이 500, H-3 응답 틀 통일, M-1 테스트 주석·검증 누락, M-2 테스트 필드 주입 정책, M-4 `DataJpaTest` 패키지 확인)은 최신 파일에서도 그대로 남아 있음.

## 1. 수정 목록 (우선순위 순)

### [높음] H-1. Java 버전이 설계와 다름
- 위치: `build.gradle:22-23` (`JavaLanguageVersion.of(21)`), `build.gradle:22` 주석 "Java 21을 사용합니다"
- 설계: `docs/spring-starter-design.md` 1절·8절은 **Java 25(Amazon Corretto 25)**, "`toolchain` languageVersion 25" 가 필수, 팀장 정정 사항.
- 영향: 빌드 대상이 21로 고정되어 설계 기준과 다른 산출물이 만들어짐.
- 함께 고칠 곳: `docs/spring-starter-steps.md` STEP 0 표(JDK 21), STEP 2 표(`Java 21`), STEP 6 표(`release version 21 not supported`).
- 확인 필요: 설계 자체가 "25"이므로 개발자가 코드를 25로 맞추는 것이 맞습니다. 다만 아키텍트가 21로 되돌리기로 결정하면 설계 문서(1절, 8절)를 먼저 고쳐야 합니다. **어느 쪽인지 아키텍트 결정 필요.**
- 관련 위험(리서치 결과): Gradle 8.14는 공식적으로 JDK 24까지 지원, JDK 25 공식 지원은 Gradle 9.1부터입니다. steps 문서 STEP 1의 `gradle wrapper --gradle-version 8.14`는 **JDK 25를 런타임으로 쓰면 실패할 수 있습니다.** Java 25로 가는 경우 Gradle 9.1 이상으로 올리는 것을 검토하세요. (설계 8절 "8.14 이상"은 하한만 적혀 있어 상한 확인이 빠져 있음)

### [높음] H-2. 상품 이름 길이 초과 시 500 응답 (버그)
- 위치: `SampleEntity.java:36` `@Column(nullable = false, length = 100)`, `SampleRequest.java:19-20` `@NotBlank`만 있고 길이 제한 없음.
- 실패 시나리오: `POST /api/samples` 에 name을 101자 이상 보내면 검증을 통과하고 INSERT에서 DB 오류 → `DataIntegrityViolationException`이 `GlobalExceptionHandler`에 잡히지 않아 **500 + Spring 기본 응답**이 나감. 설계 3절의 실패 표(400 COMMON-001)와 어긋남.
- 수정 제안: `SampleRequest`에 `@Size(max = 100, message = "...")` 추가. 또는 `GlobalExceptionHandler`에서 데이터 무결성 예외를 COMMON-001로 변환. 앞쪽(@Size)을 권장.
- 테스트 추가 제안: `SampleControllerTest`에 101자 이름 → 400 케이스.

### [높음] H-3. "모든 응답은 같은 틀(ApiResponse)" 설계 위반 경로
- 설계 1절: "성공/실패 모두 같은 틀". 현재 `GlobalExceptionHandler`가 잡는 예외는 3종뿐입니다.
- `ApiResponse` 형태가 아닌 응답이 나가는 경우:
  - `GET /api/samples/abc` → 타입 변환 실패(`MethodArgumentTypeMismatchException`) → Spring 기본 400 JSON
  - `POST` 에 `Content-Type`이 JSON이 아님 → `HttpMediaTypeNotSupportedException` → 기본 415 응답
  - 지원하지 않는 HTTP 메서드, 없는 경로 → 기본 405/404 응답 (`/error` 경로)
  - 그 밖의 모든 예상 못 한 예외 → 기본 500
- 수정 제안: `GlobalExceptionHandler`에 위 예외 처리 추가(400/415/405 → COMMON-001 등). 최소한 `Exception.class` 처리기를 두어 500도 `ApiResponse` 형태로 통일. 그러려면 `ErrorCode`에 `INTERNAL_ERROR("COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR, "...")` 추가가 필요. (주석의 "우리가 예상하지 못한 버그는 Spring 기본 500" 설명도 함께 수정)
- 판단 필요: 범위를 어디까지 할지는 아키텍트 결정. 설계 문구를 "비즈니스 예외·검증 실패는 ApiResponse, 프레임워크 기본 오류는 예외"로 좁히는 방법도 있음.

### [중간] M-1. 테스트 주석과 실제 검증 불일치 (주석 품질 + 테스트 신뢰성)
- `tests/.../sample/SampleControllerTest.java:58-60` 주석: "이 경우 Service 는 호출되지 않아야 합니다" → **검증 코드 없음.** `verify(sampleService, never()).create(any())` 추가 필요.
- `tests/.../sample/SampleServiceTest.java:44` 주석: "id=1인 샘플을 돌려줘" → 실제로는 `new SampleEntity("첫 샘플")`이라 id가 null. 주석을 "저장된 엔터티를 돌려줘"로 고치거나, 실제 id 검증을 넣을 것.
- `tests/.../sample/SampleRepositoryTest.java:28-36` `saveAndFindById`: 같은 트랜잭션 안에서 save 후 findById 하면 영속성 컨텍스트 캐시에서 꺼내므로 **DB 왕복을 증명하지 못함.** `TestEntityManager`로 `flush()` + `clear()` 후 조회하도록 수정 권장.

### [중간] M-2. 테스트 필드 주입과 설계 규칙 충돌 여부 모호
- 설계 4절: "생성자 주입을 쓴다(`@Autowired` 필드 주입 금지)".
- 테스트 클래스는 `@Autowired` 필드 주입 사용 (`StarterApplicationTests:26-27`, `SampleRepositoryTest:21-22`, `SampleControllerTest:34-35`). 주석에도 "테스트 클래스에서는 필드 주입을 씁니다"라고 적혀 있음 → 설계와 주석이 서로 다름.
- 결정 필요: (a) 설계에 "테스트 제외"를 명시하고 현 상태 유지, 또는 (b) 테스트도 생성자 주입으로 변경. 리뷰어 의견은 (a) — 테스트 프레임워크 관례상 무난하지만, 설계 문서에 한 줄 추가해야 함.

### [중간] M-3. 외부 설정값 확인 주석이 사실보다 강하게 적힘
- `build.gradle:13` 주석: "2026-11 시점 4.0.x 최신으로 확인된 값". 설계 1절은 "공식 문서를 직접 열지 못함, 개발자가 확인"이라고 적었음. 주석이 확인된 것처럼 보임.
- 리뷰어 검색 결과(2026-11-07 기준, 2차 자료): 4.0.8이 최신 패치로 보고됨(`https://isitpatched.com/spring-boot/4.0.8`), HeroDevs는 4.0.7을 최신으로 적음. 공식 릴리스 페이지에서 직접 확인되지 않음. `4.0.9`가 실제로 존재하는지는 **확인되지 않음.**
- 수정 제안: 주석을 "구현 시점 확인 필요, 4.0.9는 미확인 값"으로 바꾸고, Maven Central `org.springframework.boot:spring-boot` 최신 4.0.x로 확인 후 값과 주석을 함께 갱신.

### [중간] M-4. 패키지 경로 미확인 항목 (Spring Boot 4 확인 결과)
- `SampleRepositoryTest.java:7` `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
  - 설계 1절에는 `@WebMvcTest` 패키지만 명시. `@DataJpaTest` 경로는 설계에 없음.
  - 리뷰어 검색: 공식 마이그레이션 가이드는 모듈형 테스트 패키지가 `org.springframework.boot.<기술>.test` 형태라고 설명하나, `DataJpaTest`의 정확한 import는 **검색으로 확인되지 않음.** 현재 코드의 경로는 그 패턴과 맞지만 미검증.
  - 조치: 개발자가 컴파일 전에 Javadoc 또는 마이그레이션 가이드(`https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide`)에서 확인. 틀리면 steps STEP 6 항목대로 해당 import만 수정.
- `build.gradle:48` `spring-boot-starter-data-jpa-test`: 모듈형 스타터 이름으로 보이나 **검색으로 확인 안 됨.** 같은 방식으로 확인 필요.
- `spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`, `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `@MockitoBean`(Spring Framework 6.2+ 패키지 `org.springframework.test.context.bean.override.mockito`)은 설계 기준과 일치. 검토 범위 내 이상 없음.

### [낮음] L-1. 설계 2절 항목 누락 여부
- 설계 2절의 `src/test/resources/application-test.yml`은 "선택, 필요할 때만"이므로 없는 것이 맞음. 누락 아님.
- 설계 2절·11절 "모든 주석 한국어": 전 파일 확인 결과 한국어 준수. 단 `SampleEntity`의 `getId()`, `getName()`, `ErrorCode`의 getter에는 메서드 단위 "호출처/이유" 주석 없음(설계 7절 기준 미충족, 경미).

### [낮음] L-2. 기타 보안·운영 메모
- `steps` STEP 8: DB 비밀번호를 `application.yml`에 직접 적으라고 안내. 커밋 위험. `password: ${DB_PASSWORD}` 형태의 환경변수 사용을 권장하도록 문구 수정.
- `application.yml`의 H2 `sa` 계정과 빈 비밀번호는 실습용으로 허용. 주석에 "실습용, 운영 금지" 한 줄 추가 권장(현재 ddl-auto 쪽 주석에만 있음).
- `GET /api/samples` 전체 목록은 페이지네이션 없음. 설계 3절이 "전체 목록"으로 정했으므로 위반은 아님. 다만 `steps`나 설계에 "운영 시 페이지네이션 필요" 한 줄 권장.
- `SampleController.findOne`의 `@PathVariable Long id`는 파라미터 이름 추론에 의존(`-parameters`). Spring Boot Gradle 플러그인이 보통 자동 설정하지만, `@PathVariable("id")`로 명시하면 안전. (권장, 필수 아님)

---

## 2. 설계 일치 점검표

| 설계 항목 | 상태 | 비고 |
|---|---|---|
| Java 25 toolchain (1절, 8절) | **불일치** | H-1 |
| Spring Boot 4.0.x | 부분 | 값 미확인 (M-3) |
| webmvc / webmvc-test 스타터 사용 | 일치 | |
| 테스트 소스 위치 `src/test/java` (Gradle 기본값) | 일치 | 2차 수정(팀장 결정)으로 sourceSets 제거 |
| 롬복 미사용 | 일치 | 코드에 lombok import 없음 |
| `open-in-view: false` | 일치 | |
| `ddl-auto: create-drop` | 일치 | |
| `app.name` / `AppProperties` | 일치 | |
| 디렉터리 구조 (설계 2절) | 일치 | 필수 파일 모두 존재, `gradlew`는 STEP 1에서 생성 예정 |
| 샘플 API 3종 및 상태 코드 (3절) | 일치 | 201/200/404/400 확인 |
| 응답 틀 `ApiResponse` 모든 경우 통일 (1절, 3절) | **불일치** | H-3 |
| 계층 규칙·생성자 주입 (4절) | 일치 (본 코드 기준) | 테스트 필드 주입은 M-2 |
| 주석 규칙 (7절) | 대체로 일치 | M-1, L-1 |
| `.gitignore` (build/, .gradle/, .idea/) | 일치 | |
| 기존 Python 파일 미수정 | 일치 | `src/todo`, `tests/test_*.py`, `docs/review.md` 등 건드리지 않음 (검토 대상 아님) |
| 완료 기준 4 (`./gradlew test` 통과) | **미확인** | 환경 미설치, 개발자 실행 필요 |

## 3. steps 문서 점검

- STEP 0~3, 4(curl 예시와 기대 응답), 5, 7, 9: 코드와 대조 결과 일치. 4-1 기대 응답 JSON 필드 순서(`success, data, error`)는 `ApiResponse` record 선언 순서와 같음.
- STEP 0, 2, 6: JDK 21 표기 → H-1에 따라 함께 수정 필요.
- STEP 1: `gradle wrapper --gradle-version 8.14` → JDK 25 사용 시 호환성 문제 가능 (H-1 참고).
- STEP 6: `@WebMvcTest`/`@DataJpaTest` 패키지 확인 안내가 있어 M-4 위험에 대한 대비는 되어 있음.
- STEP 8: DB 비밀번호 직접 기재 (L-2).
- 실제 실행 결과는 검증 불가 (환경 미설치). 개발자 실행 후 STEP 4의 curl 결과를 기록해 주면 재검토 가능.

## 4. 테스트 실행 상태

- `./gradlew test` 실행하지 않음. 이유: JDK 미설치, 요청 범위가 코드 검토. (요청에 따라 개발자가 실행)
- Python 쪽 `python3 -m pytest`는 실행하지 않음 (본 검토는 Java 스타터 대상).

## 5. 다음 단계 제안

1. 아키텍트: H-1(Java 21 vs 25) 결정. 25로 가면 Gradle 버전 상향 여부도 함께 결정.
2. 개발자: H-2, H-3, M-1, M-2(정책 결정 후), M-3, L-2 수정.
3. 개발자: JDK 25 설치 후 `./gradlew test`, `./gradlew bootRun`, STEP 4 curl 결과 전달.
4. 리뷰어: 수정 후 재검토 (이 문서의 "수정 목록"을 체크하며 다시 판정).
