# 할 일 API 통합 테스트 리뷰 결과

> 작성: 리뷰어(파인 5) / 대상: `src/test/java/com/example/starter/todo/TodoIntegrationTest.java`
> 기준: `docs/todo-api-design.md`, 기존 테스트 패턴(`sample/`, `todo/` 단위·슬라이스 테스트)
> 보고 대상: 아키텍트(파인 1)

## 판정: **통과** (재검토 후 갱신)

- 초판 판정은 "조건부 통과"였습니다. 설계 4절·6절에 `TodoIntegrationTest`가 없어 8절 1번이 깨졌기 때문입니다.
- 아키텍트가 설계 문서 4절(파일 목록)과 6절(테스트 표)에 `TodoIntegrationTest.java`를 추가했습니다. 이를 확인한 결과 **설계와 구현이 일치**합니다. 조건은 해소되었습니다.
- 테스트 코드와 실행 결과는 초판과 같습니다. 반려 사유는 없습니다.

### 재검토 결과 (설계 8절 1번 기준: 파일 목록과 구현 일치)

| 구분 | 설계 4절 | 실제 파일 | 일치 |
|---|---|---|---|
| 운영 `todo/` | TodoController, TodoService, TodoRepository, TodoEntity, TodoRequest, TodoUpdateRequest, TodoResponse (7개) | 같은 7개 | 일치 |
| 테스트 `todo/` | TodoServiceTest, TodoRepositoryTest, TodoControllerTest, TodoIntegrationTest (4개) | 같은 4개 | 일치 |
| 수정 `common/ErrorCode.java` | `TODO_NOT_FOUND` 한 줄 추가 | 추가되어 있음 | 일치 |

- 설계 6절의 `TodoIntegrationTest` 설명(POST→GET→목록→PUT→DELETE→삭제 후 404)은 실제 테스트 흐름과 같습니다.
- 운영 코드 변경: 이 리뷰 문서 작성(통합 테스트 리뷰) 이후 `src/main`, `src/test/.../todo`, `build.gradle`에서 수정된 파일은 **없습니다**(`find -newer` 결과 없음). 마지막 테스트 실행(56건 통과)이 현재 코드에 그대로 유효합니다.
- 설계 1절 범위 표에는 "단위 테스트·Repository 테스트·Controller 테스트"만 적혀 있고 통합 테스트가 빠져 있습니다. 판정에는 영향이 없는 문서 정합성 사항이라 권장 사항으로 남깁니다(아래 5절 0번).

### 개발자에게 요청할 수정
- 없습니다.

## 1. 테스트 내용 검토

| 항목 | 결과 | 근거 |
|---|---|---|
| 흐름 | 통과 | POST(201, 미완료) → GET 단건 → GET 목록(1건) → PUT(200, 값 변경) → GET으로 DB 반영 재확인 → DELETE(200, data 없음) → GET(404, TODO-001). 설계 2절 명세와 일치 |
| 실제 계층 연결 | 통과 | `@SpringBootTest` + `@AutoConfigureMockMvc`로 Mock 없이 Controller → Service → Repository → H2를 모두 거침. 기존 단위·슬라이스 테스트가 못 보는 연결을 확인 |
| 데이터 격리 | 통과 | `@BeforeEach`에서 `deleteAll()`로 비움. `@SpringBootTest`는 자동 롤백이 없으므로 이 정리가 필요하고, 주석에도 그 이유가 적혀 있음 |
| 앞 단계 결과 이어 쓰기 | 통과 | 생성 응답의 `$.data.id`를 `JsonPath`로 꺼내 뒤 단계에서 씀. 하드코딩된 id에 기대지 않음 |
| 주석 기준(설계 7절) | 통과 | 한국어. 클래스·메서드마다 목적과 이유를 적음. 처음 나오는 애노테이션(`@SpringBootTest`, `@AutoConfigureMockMvc`, `@BeforeEach`)마다 설명 있음 |
| 기존 패턴 | 통과 | 패키지 경로(`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`)와 `MockMvc` 사용법이 `TodoControllerTest`와 같음 |
| 실패 시 원인 파악 | 권장 사항 참고 | 6단계가 한 테스트 메서드에 묶여 있어, 중간 단계가 깨지면 어느 단계인지 메서드명으로는 알 수 없음 (아래 3번) |

## 2. 운영 코드 변경 여부

- 리뷰 문서(`docs/todo-api-review.md`, 15:59:44 작성) 이후 수정된 파일을 찾았습니다(`find -newer`). 결과는 **`TodoIntegrationTest.java` 1개뿐**입니다. `src/main`, `build.gradle`, `docs/`는 바뀌지 않았습니다.
- 운영 코드 `src/main/java/com/example/starter/todo/*`, `common/ErrorCode.java`는 15:55 수정이고, 이는 리뷰 문서 작성 전입니다. 리뷰 때 읽은 내용과 같은 상태라고 판단합니다. 다만 이 시각 이전 상태와의 비교는 git이 없어서 할 수 없습니다.
- 결론: **이번 통합 테스트 추가로 운영 코드는 바뀌지 않았습니다.**

## 3. 테스트 실행 결과

명령: 프로젝트 루트에서 `./gradlew test --rerun` (캐시 무시. 기본 `./gradlew test`는 `UP-TO-DATE`로 실제 실행되지 않을 수 있음)

- `BUILD SUCCESSFUL`, `:test` 실제 실행.
- 리포트 기준 **총 56건, 실패 0, 오류 0, 건너뜀 0**. 개발자 보고(56건)와 일치합니다.

| 테스트 클래스 | 건수 | 결과 |
|---|---|---|
| `todo.TodoIntegrationTest` (신규) | 1 | 통과 |
| `todo.TodoServiceTest` | 8 | 통과 |
| `todo.TodoRepositoryTest` | 4 | 통과 |
| `todo.TodoControllerTest` | 16 | 통과 |
| `sample.SampleServiceTest` | 4 | 통과 |
| `sample.SampleRepositoryTest` | 2 | 통과 |
| `sample.SampleControllerTest` | 11 | 통과 |
| `common.GlobalExceptionHandlerTest` | 8 | 통과 |
| `StarterApplicationTests` | 2 | 통과 |

## 4. 발견 사항

### 4-1. [설계 불일치] 파일 목록·테스트 기준 미반영 → **해소됨**
- 초판에서는 설계 4절·6절에 `TodoIntegrationTest.java`가 없었습니다.
- 아키텍트가 4절 파일 목록과 6절 테스트 표에 추가했습니다. 8절 1번 기준으로 다시 대조해 일치함을 확인했습니다.

### 4-2. [위험, 현재는 재현되지 않음] H2 메모리 DB 공유
- `application.yml`의 URL이 `jdbc:h2:mem:starter`로 고정되어 있습니다.
- `@SpringBootTest`(이 테스트)와 `@DataJpaTest`(`TodoRepositoryTest`, `SampleRepositoryTest`)는 서로 다른 컨텍스트이지만, 같은 JVM 안에서 **같은 메모리 DB 이름을 공유**할 수 있습니다.
- `TodoRepositoryTest.findAllReturnsEverySavedRow`는 "테이블이 비어 있다"를 가정하고 `hasSize(2)`를 검사합니다. 이 테스트 중간에 실패해 행이 남으면, 다른 컨텍스트에서 이 가정이 깨질 수 있습니다.
- 이번 실행에서는 실패가 없어 재현되지 않았습니다. 로그에서도 두 컨텍스트가 각각 `drop table`과 커넥션 종료를 하는 것만 확인됩니다.
- 권장: `spring.datasource.url`을 테스트에서 고유 이름으로 두거나, `@AfterEach`에서도 정리합니다. 당장 고치지 않아도 되지만, 간헐 실패가 보이면 이 원인부터 봅니다.

## 5. 권장 사항 (반려 사유 아님)

0. **설계 1절 범위 표 보강(문서 정합성)**: 1절의 "단위 테스트·Repository 테스트·Controller 테스트"에 통합 테스트를 덧붙이면 4절·6절과 표현이 맞습니다. 아키텍트 판단에 맡깁니다.
1. **단계별 메시지 보강**: 각 `perform` 옆에 `.andDo(print())`는 노이즈가 크므로 쓰지 말고, 단계가 많아진다면 `@Nested` 또는 단계별 메서드로 나누는 것을 고려합니다. 지금 6단계는 이 정도 크기에서는 허용됩니다.
2. **검증 실패 경로 한 개 추가 고려**: 흐름 테스트이므로 필수는 아니지만, 실제 스택에서 `POST` 빈 제목 → 400 한 건을 넣으면 계층 연결의 검증 경로도 확인됩니다.
3. **`assertThat(id).isNotNull()`**: 응답 파싱에서 `id`가 없으면 `JsonPath.read`에서 이미 실패하므로 중복입니다. 실제 값 검증이 더 의미가 있습니다. 무해하므로 그대로 둬도 됩니다.

## 6. 결론

- **판정: 통과.** 설계 8절 완료 기준 1번(파일 목록과 구현 일치)을 충족합니다.
- 테스트 코드: 정상. 흐름·주석·격리 모두 기준을 충족합니다.
- 운영 코드: 이번 작업으로 변경되지 않았고, 이후에도 변경되지 않았습니다.
- 실행: 56건 전부 통과(초판 실행 결과. 그 뒤 코드 변경이 없어 유효합니다).
- 남은 항목: 권장 사항 0번(설계 1절 표 보강)과 4-2번(H2 메모리 DB 공유 위험, 재현 안 됨). 둘 다 머지를 막지 않습니다.
