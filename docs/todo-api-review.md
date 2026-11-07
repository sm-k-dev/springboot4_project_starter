# 할 일(Todo) CRUD API 리뷰 결과

> 작성: 리뷰어(파인 5) / 대상 설계: `docs/todo-api-design.md` / 보고 대상: 아키텍트(파인 1)
> 검토 기준: 설계 문서 8절 "완료 기준"
> 검토 방법: 설계 문서와 구현·테스트 코드를 직접 읽고 대조했습니다. `./gradlew test`를 직접 실행했습니다.

## 판정: **통과**

8절 완료 기준 5개를 모두 충족합니다. 반려 사유가 되는 결함은 없습니다.
아래 "확인 한계"와 "권장 사항"은 머지를 막지 않는 참고 사항입니다.

## 1. 완료 기준 체크

| # | 완료 기준 | 결과 | 근거 |
|---|---|---|---|
| 1 | 5개 API가 2절 명세대로 동작하고, 파일 목록(4절)과 일치한다 | 통과 | 아래 1-1, 1-2 참고 |
| 2 | `ErrorCode`에 `TODO_NOT_FOUND`가 추가되고, 기존 값은 바뀌지 않았다 | 통과 (확인 한계 있음) | `ErrorCode.java` 에 `TODO_NOT_FOUND("TODO-001", NOT_FOUND, "할 일을 찾을 수 없습니다")` 추가 확인. 기존 `COMMON-001~004`, `COMMON-500`, `SAMPLE-001` 값 정상. 원본 비교용 git 이력이 없어 "변경 전과 동일"은 직접 대조하지 못함 |
| 3 | 모든 새 파일의 주석이 한국어이며 7절 기준을 지킨다 | 통과 | 신규 10개 파일(main 7 + test 3) 모두 한국어 주석. 클래스·메서드마다 "누가 호출하나/왜"를 적음. 처음 나오는 애노테이션마다 한 줄 설명 있음 |
| 4 | `./gradlew test`가 프로젝트 루트에서 통과한다 (기존 Sample 테스트, GlobalExceptionHandlerTest 포함) | 통과 | 아래 2절 참고 |
| 5 | Python 파일과 기존 설계 문서는 변경되지 않았다 | 통과 (확인 한계 있음) | `src/todo/*.py`, `tests/*.py` 수정 시각이 모두 14:19~14:27로, 이번 Todo 작업 이전. `docs/spring-starter-steps.md`는 15:14 수정이라 이번 작업 범위 밖이지만, git이 없어 변경 내용까지는 확인 못함 |

### 1-1. 5개 API 명세 대조 (설계 2절)

| API | 구현 | 테스트 | 판정 |
|---|---|---|---|
| POST `/api/todos` → 201 | `TodoController.create`, `ResponseEntity.status(CREATED)` | `createReturns201WithBody` | 일치 |
| 제목 빈 값·공백·101자 → 400 COMMON-001 | `TodoRequest`의 `@NotBlank`, `@Size(max=100)` | `createWithBlankTitle…`, `createWithTitleOver100Chars…` | 일치 |
| GET `/api/todos/{id}` → 200 / 404 TODO-001 / 숫자 아님 400 | `findOne`, `findOrThrow` | `findOneReturns200…`, `findOneMissing…`, `findOneWithNonNumericId…` | 일치 |
| GET `/api/todos` → 200 | `findAll` | `findAllReturns200WithList` | 일치 |
| PUT `/api/todos/{id}` → 200 / 404 / 검증 400 | `update`, `TodoUpdateRequest` (`@NotBlank`, `@Size`, `@NotNull Boolean completed`) | `updateReturns200…`, `updateMissing…`, `updateWithoutCompleted…`, `updateWithBlankTitle…`, `updateWithTitleOver100Chars…` | 일치 |
| DELETE `/api/todos/{id}` → 200 (data null) / 404 | `delete`, `ApiResponse.ok(null)` | `deleteReturns200WithEmptyData`, `deleteMissing…` | 일치 |

- 수정은 PUT 전체 교체, 삭제는 200 + `ApiResponse`로 설계대로 구현되었습니다.
- 생성 요청에 `completed`가 없고 항상 `false`로 시작하는 규칙은 `TodoRequest`(필드 없음)와 `TodoEntity(String)` 생성자로 지켜집니다.

### 1-2. 파일 목록 대조 (설계 4절)

- 설계에 적힌 신규 파일 7개(`TodoController`, `TodoService`, `TodoRepository`, `TodoEntity`, `TodoRequest`, `TodoUpdateRequest`, `TodoResponse`)가 모두 존재합니다.
- 설계에 적힌 테스트 3개(`TodoServiceTest`, `TodoRepositoryTest`, `TodoControllerTest`)가 모두 존재합니다.
- 설계에 없는 파일은 todo 패키지에 추가되지 않았습니다.
- 수정 파일은 `ErrorCode.java` 한 곳뿐입니다. `GlobalExceptionHandler`, `application.yml`, `build.gradle`은 손대지 않았습니다.

## 2. 테스트 실행 결과

명령: 프로젝트 루트에서 `./gradlew test --rerun`

- 처음 `./gradlew test`를 실행했을 때는 Gradle 캐시로 `UP-TO-DATE`가 떠서 테스트가 실제로 돌지 않았습니다. 그래서 `--rerun`(캐시 무시)으로 다시 실행했습니다. 결과: `BUILD SUCCESSFUL`, `:test` 실제 실행.
- 테스트 리포트(`build/test-results/test/*.xml`) 기준 총 **55건, 실패 0, 오류 0, 건너뜀 0**.

| 테스트 클래스 | 건수 | 결과 |
|---|---|---|
| `todo.TodoServiceTest` | 8 | 통과 |
| `todo.TodoRepositoryTest` | 4 | 통과 |
| `todo.TodoControllerTest` | 16 | 통과 |
| `sample.SampleServiceTest` | 4 | 통과 |
| `sample.SampleRepositoryTest` | 2 | 통과 |
| `sample.SampleControllerTest` | 11 | 통과 |
| `common.GlobalExceptionHandlerTest` | 8 | 통과 |
| `StarterApplicationTests` | 2 | 통과 |

- 설계 6절 테스트 기준(서비스 4가지 이상, 리포지토리 저장·조회·수정·삭제, 컨트롤러 5개 API와 검증 실패, 경계값 100자·101자)을 모두 커버합니다.
- `TodoRepositoryTest`의 `@DataJpaTest` import 경로가 `SampleRepositoryTest`와 같습니다(설계 9절 준수).

## 3. 관점별 검토 (버그·보안·성능·가독성 / 계층·예외·검증)

| 관점 | 검토 결과 |
|---|---|
| 계층 분리 | Controller는 전달만 하고, 없음 판단은 Service(`findOrThrow`)에 있습니다. Entity는 응답으로 나가지 않고 `TodoResponse`로 변환됩니다. 통과 |
| 생성자 주입 | Controller, Service 모두 생성자 주입입니다. 필드 `@Autowired`는 테스트 클래스에만 있습니다. 통과 |
| 트랜잭션 범위 | 조회는 `readOnly = true`, 생성·수정·삭제는 `@Transactional`입니다. `update`는 같은 트랜잭션 안에서 엔터티를 바꾸고 JPA dirty checking으로 반영합니다. 통과 |
| null 처리 | `completed`는 `Boolean` + `@NotNull`로 생략을 막고, 언박싱은 검증 뒤에 일어납니다. `findById`는 `Optional`을 `orElseThrow`로 처리합니다. 통과 |
| 입력값 검증 | `@Valid`가 POST·PUT 본문에 붙어 있습니다. 제목은 `@NotBlank`와 `@Size(max=100)`, 엔터티 컬럼은 `length = 100`으로 같은 기준입니다. 통과 |
| 예외 처리 | 없음은 `BusinessException(TODO_NOT_FOUND)`, 나머지는 기존 `GlobalExceptionHandler`가 응답 형식을 맞춥니다. 핸들러는 수정하지 않았습니다. 통과 |
| mass assignment | 요청은 `record`로 받고 필드가 `title`, `completed`뿐이라 `id` 등을 클라이언트가 바꿀 수 없습니다. 통과 |
| SQL 인젝션 | JPA 기본 메서드만 쓰고 문자열 쿼리가 없습니다. 통과 |
| 성능 | 목록 조회는 `findAll()` 전체 조회입니다. 설계상 페이징 제외 범위이므로 허용합니다. 통과 |
| 가독성 | 메서드·클래스 주석이 "왜"를 설명합니다. 중복되는 제목 검증은 주석에 공통화 방향이 적혀 있습니다. 통과 |

## 4. 확인 한계

- **ErrorCode 변경 전후 비교**: git이 없어서 원본과 diff를 떠 보지 못했습니다. 현재 파일의 기존 값은 정상이며, 이번 작업 범위 밖이라고 판단했습니다.
- **Python·설계 문서 불변**: 수정 시각으로만 확인했습니다. 내용 비교는 못 했습니다.
- **end-to-end 미검증**: Controller 테스트는 Service를 mock으로 바꾸고, Repository 테스트는 Service 없이 진행합니다. 실제 `Controller → Service → Repository → H2`를 한 번에 잇는 통합 테스트는 없습니다. 설계 6절이 요구하지 않은 항목이라 반려 사유는 아닙니다.

## 5. 권장 사항 (반려 사유 아님, 다음 작업 때 참고)

1. **통합 테스트 1개 추가 고려**: `@SpringBootTest` + `MockMvc`로 POST → GET → PUT → DELETE 흐름을 한 번 돌리면, 계층 연결 실수를 잡을 수 있습니다. 필수는 아닙니다.
2. **제목 앞뒤 공백 정책 결정**: 현재 `"  우유  "`는 공백이 그대로 저장됩니다(공백만 있는 값은 거절). 설계에 trim 규칙이 없으므로 그대로 두어도 됩니다. 정책이 필요해지면 설계에 명시하세요.
3. **제목 검증 중복**: `TodoRequest`와 `TodoUpdateRequest`에 같은 두 애노테이션이 있습니다. 주석에 이미 공통화 가능성이 적혀 있으니, 제목 규칙이 늘어날 때 함께 정리하면 됩니다.

## 6. 결론

- 반려 사유 없음. 설계 8절 완료 기준 5개 충족.
- 테스트 55건 전부 통과(캐시 무시 재실행으로 확인).
- 개발자가 수정할 항목은 없습니다.
