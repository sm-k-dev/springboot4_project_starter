# Spring Boot 4 스타터

Java 25 + Spring Boot 4.0.9 로 만든 웹 API 시작 프로젝트입니다. 공통 응답·예외 처리 구조와 샘플 API, 할 일(Todo) CRUD API가 들어 있어서, 여기서 복사해 새 프로젝트를 시작하거나 학습용으로 읽어 볼 수 있습니다.

## 기술 스택

| 항목 | 버전 / 내용 |
|---|---|
| JDK | 25 (Amazon Corretto 25 권장, Gradle toolchain으로 고정) |
| 빌드 | Gradle Wrapper (`./gradlew`) |
| 프레임워크 | Spring Boot 4.0.9 (`starter-webmvc`, `starter-data-jpa`, `starter-validation`) |
| DB | H2 인메모리 (실습용, 재시작하면 데이터가 사라집니다) |
| 테스트 | JUnit 5, spring-boot-starter-test, MockMvc |

## 프로젝트 구조

```
src/main/java/com/example/starter/
├── StarterApplication.java   # 실행 진입점
├── common/                   # 공통 응답(ApiResponse), 에러 코드(ErrorCode), 예외 처리
├── config/                   # app.* 설정 값을 읽는 AppProperties
├── sample/                   # 샘플 API (생성·단건 조회·목록)
└── todo/                     # 할 일 API (CRUD 5개)
src/main/resources/application.yml
src/test/java/...             # 단위·슬라이스·통합 테스트
docs/                         # 설계 문서와 따라 하기 가이드
```

각 패키지는 `Controller → Service → Repository` 순서로 역할을 나눕니다. Controller는 요청을 받아 넘기고 응답만 돌려주며, 업무 판단은 Service가 맡습니다.

## 실행 방법

프로젝트 루트에서 실행합니다.

```bash
# 테스트
./gradlew test

# 빌드
./gradlew build

# 실행 (기본 포트 8080)
./gradlew bootRun
```

Windows에서는 `gradlew.bat` 을 사용합니다.

## API

### 공통 응답 형식

성공과 실패 모두 같은 모양으로 응답합니다.

```json
{ "success": true,  "data": { ... },   "error": null }
{ "success": false, "data": null,      "error": { "code": "TODO-001", "message": "할 일을 찾을 수 없습니다" } }
```

### 할 일 API (`/api/todos`)

| 메서드 | 경로 | 요청 본문 | 성공 | 실패 |
|---|---|---|---|---|
| POST | `/api/todos` | `{"title": "우유 사기"}` | 201 | 제목 비어 있음 / 100자 초과 → 400 |
| GET | `/api/todos` | - | 200 (목록) | - |
| GET | `/api/todos/{id}` | - | 200 | 없으면 404 (`TODO-001`) |
| PUT | `/api/todos/{id}` | `{"title": "우유 두 개 사기", "completed": true}` | 200 | 없으면 404, 검증 실패 → 400 |
| DELETE | `/api/todos/{id}` | - | 200 (data: null) | 없으면 404 |

수정은 PUT(전체 교체)입니다. `title`과 `completed`를 모두 보내야 합니다.

### 샘플 API (`/api/samples`)

| 메서드 | 경로 | 요청 본문 | 성공 |
|---|---|---|---|
| POST | `/api/samples` | `{"name": "첫 샘플"}` | 201 |
| GET | `/api/samples` | - | 200 (목록) |
| GET | `/api/samples/{id}` | - | 200, 없으면 404 (`SAMPLE-001`) |

### 요청 예시

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "우유 사기"}'

curl http://localhost:8080/api/todos
```

## 에러 코드

| 코드 | HTTP | 의미 |
|---|---|---|
| COMMON-001 | 400 | 입력값이 올바르지 않음 (검증 실패, JSON 형식 오류 등) |
| COMMON-002 | 405 | 지원하지 않는 HTTP 메서드 |
| COMMON-003 | 415 | 지원하지 않는 Content-Type |
| COMMON-004 | 404 | 없는 경로 |
| COMMON-500 | 500 | 서버 내부 오류 |
| SAMPLE-001 | 404 | 샘플을 찾을 수 없음 |
| TODO-001 | 404 | 할 일을 찾을 수 없음 |

새 에러를 추가하려면 `common/ErrorCode.java` 에 한 줄을 넣고, Service에서 `BusinessException(ErrorCode.XXX)` 을 던지면 됩니다.

## 설정

설정은 `src/main/resources/application.yml` 에 있습니다.

- `spring.datasource`: 기본은 H2 메모리 DB입니다. 계정 `sa`, 비밀번호 없음은 **실습용 값**입니다.
- `spring.jpa.hibernate.ddl-auto: create-drop`: 실행할 때 테이블을 만들고 종료할 때 지웁니다. 실서비스에서는 쓰지 마세요.
- `app.*`: 직접 정한 설정 값이며 `AppProperties` 가 읽습니다.

실제 DB에 연결할 때는 비밀번호를 파일에 적지 말고 환경변수로 받습니다.

```bash
export DB_PASSWORD=비밀번호
```

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}
```

## 문서

| 문서 | 내용 |
|---|---|
| `docs/spring-starter-steps.md` | 처음부터 따라 하는 실행 가이드 |
| `docs/spring-starter-design.md` | 스타터 설계 배경 |
| `docs/todo-api-design.md` | 할 일 API 설계 |

## 주의 사항

- 이 프로젝트의 DB 설정(H2 메모리, `create-drop`, 빈 비밀번호)은 학습용입니다.
- `.claude/settings.local.json` 같은 개인 설정은 공유 대상이 아닙니다.
