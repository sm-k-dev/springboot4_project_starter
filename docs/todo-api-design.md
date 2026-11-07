# 할 일(Todo) CRUD API 설계

> 작성: 아키텍트(파인 1) / 요청: 팀장 / 구현: 개발자(파인 4) / 리뷰: 리뷰어(파인 5)
> 목적: 기존 Spring Boot 스타터(`src/main/java/com/example/starter`)에 할 일 CRUD API를 추가한다.
> 기존 `sample/` 패키지의 구조·주석 스타일·응답 형태·예외 처리 방식을 그대로 따른다.

## 1. 범위

| 포함 | 제외 |
|---|---|
| 할 일 생성·단건 조회·전체 목록·수정·삭제 (CRUD 5개 API) | 로그인·권한(인증/인가) |
| 입력 검증(제목 필수, 100자 이하) | 페이징·정렬·검색 |
| 404/400 등 에러 응답 (기존 `ErrorCode`, `GlobalExceptionHandler` 재사용) | 사용자별 데이터 분리 |
| 단위 테스트·Repository 테스트·Controller 테스트, 전체 흐름 통합 테스트 1개 | 기존 `docs/spring-starter-steps.md` 수정 (이번 범위 아님) |

기존 Python 할 일 앱(`src/todo/`, `tests/*.py`, `docs/design.md` 등)은 **건드리지 않는다.**

## 2. API 명세

기본 주소: `/api/todos`. 모든 응답은 기존 `ApiResponse` 형태를 따른다.

| # | 메서드 | 경로 | 요청 본문 | 성공 | 실패 |
|---|---|---|---|---|---|
| 1 | POST | `/api/todos` | `{"title": "우유 사기"}` | 201, `ApiResponse<TodoResponse>` | 제목 비어 있음/100자 초과 → 400 (`COMMON-001`) |
| 2 | GET | `/api/todos/{id}` | - | 200, `ApiResponse<TodoResponse>` | 없으면 404 (`TODO-001`), id가 숫자가 아니면 400 (`COMMON-001`) |
| 3 | GET | `/api/todos` | - | 200, `ApiResponse<List<TodoResponse>>` | - |
| 4 | PUT | `/api/todos/{id}` | `{"title": "우유 두 개 사기", "completed": true}` | 200, `ApiResponse<TodoResponse>` | 없으면 404 (`TODO-001`), 검증 실패 → 400 (`COMMON-001`) |
| 5 | DELETE | `/api/todos/{id}` | - | 200, `ApiResponse<Void>` (data: null) | 없으면 404 (`TODO-001`) |

**수정 방식은 PUT(전체 교체)** 으로 한다. 요청 본문의 `title`, `completed` 두 값이 모두 필요하다. (부분 수정 PATCH는 이번 범위에서 제외)

**삭제 응답은 204가 아니라 200 + `ApiResponse`** 로 한다. 모든 응답을 같은 모양으로 유지하기 위한 결정이다.

### 응답 예시
```json
{ "success": true,  "data": { "id": 1, "title": "우유 사기", "completed": false }, "error": null }
{ "success": false, "data": null, "error": { "code": "TODO-001", "message": "할 일을 찾을 수 없습니다" } }
```

## 3. 검증 기준

| 대상 | 규칙 | 메시지 |
|---|---|---|
| `title` (생성·수정 공통) | `@NotBlank`: null, 빈 문자열, 공백만 있는 값 거절 | `제목은 비어 있을 수 없습니다` |
| `title` | `@Size(max = 100)`: 자바 `String.length()` 기준 | `제목은 100자 이하여야 합니다` |
| `completed` (수정만) | `@NotNull`: 값 생략 시 거절 | `완료 여부는 필수입니다` |

- 검증 실패는 `GlobalExceptionHandler`가 `COMMON-001`(400)로 통일한다. 기존 동작 그대로이며 handler는 수정하지 않는다.
- 생성 요청의 `completed`는 받지 않는다. 새 할 일은 항상 `completed = false`로 시작한다.

## 4. 파일 목록

```
src/main/java/com/example/starter/
├── common/
│   └── ErrorCode.java                 [수정] TODO_NOT_FOUND("TODO-001", NOT_FOUND, "할 일을 찾을 수 없습니다") 한 줄 추가
└── todo/                              [신규 패키지]
    ├── TodoController.java            REST API (/api/todos), 5개 엔드포인트
    ├── TodoService.java               업무 로직, 트랜잭션, 없으면 BusinessException(TODO_NOT_FOUND)
    ├── TodoRepository.java            JpaRepository<TodoEntity, Long>
    ├── TodoEntity.java                @Entity, 테이블 "todos" (id, title VARCHAR(100) NOT NULL, completed BOOLEAN NOT NULL)
    ├── TodoRequest.java               생성 요청 record: title (@NotBlank, @Size(max=100))
    ├── TodoUpdateRequest.java         수정 요청 record: title (@NotBlank, @Size(max=100)), completed (@NotNull)
    └── TodoResponse.java              응답 record: id, title, completed + from(TodoEntity)

src/test/java/com/example/starter/
└── todo/
    ├── TodoServiceTest.java           Mockito 단위 테스트 (정상/없는 id 예외)
    ├── TodoRepositoryTest.java        @DataJpaTest: 저장·조회·수정·삭제
    ├── TodoControllerTest.java        @WebMvcTest + @MockitoBean + MockMvc: 5개 API, 상태 코드, 검증 실패
    └── TodoIntegrationTest.java       [추가] @SpringBootTest + @AutoConfigureMockMvc, H2: POST→GET→목록→PUT→DELETE→삭제 후 404 전체 흐름
```

`ErrorCode.java` 외에는 기존 파일을 수정하지 않는다. `application.yml`, `build.gradle`도 변경하지 않는다. (H2·JPA 설정이 이미 있음)

## 5. 계층 설계

- **Controller** (`TodoController`): 요청을 받아 Service에 넘기고 응답을 만든다. 업무 판단은 하지 않는다.
- **Service** (`TodoService`): 조회·수정·삭제 시 id가 없으면 `BusinessException(ErrorCode.TODO_NOT_FOUND)`을 던진다. 조회는 `@Transactional(readOnly = true)`, 변경은 `@Transactional`.
- **Repository** (`TodoRepository`): `JpaRepository` 상속만 한다. 메서드는 기본(`save`, `findById`, `findAll`, `deleteById` 등)을 쓴다.
- **Entity** (`TodoEntity`): 기존 `SampleEntity`처럼 `protected` 기본 생성자 + 생성자 주입을 쓴다.
  - 수정을 위해 `update(String title, boolean completed)` 메서드 하나만 둔다. setter는 만들지 않는다.
  - 기본 키는 `GenerationType.IDENTITY`.
- **Response** (`TodoResponse`): Entity를 직접 노출하지 않고 `from(TodoEntity)`로 변환한다.
- 생성자 주입만 사용한다. 테스트 클래스만 `@Autowired` 필드 주입을 허용한다(설계 문서 `spring-starter-design.md` 4절 규칙).

## 6. 테스트 기준

| 테스트 | 확인 내용 |
|---|---|
| `TodoServiceTest` (Mockito) | 생성 시 `completed=false`로 저장, 단건 조회 성공, 없는 id 조회/수정/삭제 시 `TODO_NOT_FOUND` 예외, 목록 변환 |
| `TodoRepositoryTest` (`@DataJpaTest`) | 저장 후 id 부여, 조회, `update` 후 반영, 삭제 후 조회 불가 |
| `TodoControllerTest` (`@WebMvcTest`, `@MockitoBean TodoService`) | POST 201, GET 단건 200/404, GET 목록 200, PUT 200/404, DELETE 200/404, title 빈 값·101자 → 400(`COMMON-001`), PUT에서 `completed` 누락 → 400, 숫자가 아닌 id → 400 |
| `TodoIntegrationTest` (`@SpringBootTest`, `@AutoConfigureMockMvc`, H2) | Service·Repository를 실제로 연결한 상태에서 POST(201) → GET 단건(200) → GET 목록(포함) → PUT(200, 값 반영) → DELETE(200) → 삭제 후 GET(404, `TODO-001`) 한 흐름을 검증. 운영 코드는 수정하지 않는다 |

- 제목 100자는 통과(201 또는 200), 101자는 400이 나야 한다.
- Controller 테스트 클래스에 `@WebMvcTest`를 쓸 때는 기존 `SampleControllerTest`의 설정 방식을 그대로 따른다.

## 7. 주석 기준

기존 `sample/` 코드와 같다.
- 클래스·메서드마다 **누가 호출하는지, 왜 필요한지**를 한국어로 적는다.
- 처음 나오는 애노테이션(`@Entity`, `@Transactional`, `@WebMvcTest` 등)은 한 줄 설명을 붙인다.
- "무엇을"이 아니라 "왜"를 적는다. 코드를 그대로 옮기는 주석은 쓰지 않는다.

## 8. 완료 기준 (리뷰 통과 조건)

1. 5개 API가 2절 명세대로 동작하고, 파일 목록(4절)과 일치한다.
2. `ErrorCode`에 `TODO_NOT_FOUND`가 추가되고, 기존 코드 값은 바뀌지 않았다.
3. 모든 새 파일의 주석이 한국어이며 기준(7절)을 지킨다.
4. `./gradlew test`가 프로젝트 루트에서 통과한다. (기존 `SampleXxxTest`, `GlobalExceptionHandlerTest`도 그대로 통과해야 한다)
5. Python 파일과 기존 설계 문서는 변경되지 않았다.

## 9. 구현 시 주의

- 실행 명령은 프로젝트 루트(`~/claude/project`)에서 `./gradlew test` 한 번만 쓴다. 환경(JDK·Gradle)은 바꾸지 않는다. 실행이 안 되면 그대로 보고한다.
- 패키지 경로(`org.springframework.boot.webmvc.test.autoconfigure.*` 등)는 기존 `SampleControllerTest`의 import를 그대로 따른다. 새로 추측해서 쓰지 않는다.
