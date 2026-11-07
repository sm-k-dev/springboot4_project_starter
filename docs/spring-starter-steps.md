# Spring Boot 4 스타터 따라 하기

> 이 문서만 순서대로 따라 하면 샘플 API가 도는 Spring Boot 4 프로젝트를 실행하고 테스트할 수 있습니다.
> 설계 배경은 `docs/spring-starter-design.md` 를 보세요.

---

## STEP 0. 준비물 설치 (한 번만)

| 도구 | 버전 | 확인 명령 |
|---|---|---|
| JDK | **25** (Amazon Corretto 25) | `java -version` → `25.x` 가 나와야 합니다 |
| Gradle | **9.9.0** | `gradle -version` → `Gradle 9.9.0` 이 나와야 합니다 (없어도 STEP 1에서 Gradle Wrapper를 만들면 됩니다) |

- JDK 25 설치: Amazon Corretto 25 배포판을 설치합니다. 설치 후 터미널을 새로 열어 `java -version` 을 확인하세요.
- Gradle 설치: 공식 사이트(services.gradle.org)의 9.9.0 배포판을 받아 압축을 풀고 `bin` 폴더를 PATH에 넣거나, 설치 안내를 따릅니다.
- 설치 후 확인 순서: `java -version` → `gradle -version` → STEP 1.

> 참고: 이 스타터의 빌드는 `build.gradle` 의 `toolchain` 에 맞춰 JDK 25를 사용합니다. 다른 JDK 버전이 설치되어 있어도 Gradle이 25를 찾아 씁니다.
> 처음 실행할 때 오류가 나면 STEP 6의 "자주 나는 오류"를 먼저 보세요.

---

## STEP 1. Gradle Wrapper 만들기 (한 번만)

Wrapper(`gradlew`)는 "정해진 Gradle 버전을 자동으로 내려받아 쓰는 도구"입니다. 팀원마다 Gradle 버전이 달라서 생기는 문제를 막아 줍니다.

프로젝트 루트(`build.gradle` 이 있는 폴더)에서 실행합니다.

```bash
gradle wrapper --gradle-version 9.9.0
```

- 버전은 **9.9.0** 으로 고정합니다. JDK 25는 Gradle 9.1 이상에서 공식 지원되므로 8.x 계열(예: 8.14)을 쓰지 마세요.

- 완료되면 `gradlew`, `gradlew.bat`, `gradle/wrapper/` 폴더가 생깁니다.
- 이 파일들은 저장소에 함께 올려야 합니다. (이번 스타터에는 아직 없으므로 이 단계에서 만듭니다)
- 이후에는 `gradle` 대신 `./gradlew`(Windows는 `gradlew.bat`)를 씁니다.

---

## STEP 2. 프로젝트 파일 살펴보기

이 스타터에 이미 들어 있는 파일입니다. 한 번씩 열어 보세요.

| 파일 | 역할 |
|---|---|
| [build.gradle](../build.gradle) | 라이브러리 목록, Java 25, 테스트 폴더는 Gradle 기본값(`src/test/java`) |
| [settings.gradle](../settings.gradle) | 프로젝트 이름 `starter` |
| [application.yml](../src/main/resources/application.yml) | H2 DB, JPA, `app.name` 설정 |
| [StarterApplication.java](../src/main/java/com/example/starter/StarterApplication.java) | 프로그램 시작점 (`main`) |
| [AppProperties.java](../src/main/java/com/example/starter/config/AppProperties.java) | `app.*` 설정을 자바 객체로 묶기 |
| [ApiResponse.java](../src/main/java/com/example/starter/common/ApiResponse.java) | 모든 응답의 공통 모양 |
| [ErrorCode.java](../src/main/java/com/example/starter/common/ErrorCode.java) | 에러 코드 목록 (`COMMON-001`, `SAMPLE-001`) |
| [BusinessException.java](../src/main/java/com/example/starter/common/BusinessException.java) | 업무 규칙 위반 예외 |
| [GlobalExceptionHandler.java](../src/main/java/com/example/starter/common/GlobalExceptionHandler.java) | 예외를 공통 응답으로 바꾸기 |
| [SampleController.java](../src/main/java/com/example/starter/sample/SampleController.java) | HTTP 요청 입구 (`/api/samples`) |
| [SampleService.java](../src/main/java/com/example/starter/sample/SampleService.java) | 업무 로직 |
| [SampleRepository.java](../src/main/java/com/example/starter/sample/SampleRepository.java) | DB 접근 |
| [SampleEntity.java](../src/main/java/com/example/starter/sample/SampleEntity.java) | DB 테이블 `samples` 와 연결된 클래스 |
| [SampleRequest.java](../src/main/java/com/example/starter/sample/SampleRequest.java) / [SampleResponse.java](../src/main/java/com/example/starter/sample/SampleResponse.java) | 요청/응답 본문 모양 |

**계층 규칙 한 줄 요약:** `Controller → Service → Repository` 한 방향으로만 호출합니다. Controller에 업무 로직을 쓰지 않습니다.

---

## STEP 3. 앱 실행하기

```bash
./gradlew bootRun
```

- 콘솔에 `Started StarterApplication` 이 보이면 성공입니다. 기본 주소는 `http://localhost:8080` 입니다.
- 종료는 터미널에서 `Ctrl + C` 입니다.

---

## STEP 4. API 호출해 보기

다른 터미널을 열고 아래를 순서대로 실행합니다. (앱은 STEP 3에서 켜 둔 상태여야 합니다)

> 샘플 이름 규칙: `name` 은 **1~100자**이며 **공백만으로는 안 됩니다.** 글자 수는 자바 문자열 길이로 셉니다(한글·영문 1자, 일부 이모지는 2). 규칙을 어기면 400 `COMMON-001` 이 응답됩니다.

**4-1. 샘플 만들기 (201 Created)**
```bash
curl -i -X POST http://localhost:8080/api/samples \
  -H "Content-Type: application/json" \
  -d '{"name": "첫 샘플"}'
```
기대 응답 본문: `{"success":true,"data":{"id":1,"name":"첫 샘플"},"error":null}`

**4-2. 한 개 조회 (200 OK)**
```bash
curl -i http://localhost:8080/api/samples/1
```

**4-3. 없는 샘플 조회 (404 Not Found)**
```bash
curl -i http://localhost:8080/api/samples/999
```
기대 응답 본문: `{"success":false,"data":null,"error":{"code":"SAMPLE-001","message":"샘플을 찾을 수 없습니다"}}`

**4-4. 이름이 비어 있을 때 (400 Bad Request)**
```bash
curl -i -X POST http://localhost:8080/api/samples \
  -H "Content-Type: application/json" \
  -d '{"name": "  "}'
```
기대 응답 본문: `{"success":false,"data":null,"error":{"code":"COMMON-001","message":"입력값이 올바르지 않습니다"}}`

**4-5. 전체 목록 (200 OK)**
```bash
curl -i http://localhost:8080/api/samples
```

> Windows 사용자는 `curl` 대신 PowerShell의 `Invoke-RestMethod` 를 쓰거나, 브라우저로 GET 주소(4-2, 4-5)만 열어 봐도 됩니다.

---

## STEP 5. 테스트 실행하기

```bash
./gradlew test
```

- 테스트 파일은 모두 `src/test/java/` 아래에 있습니다.

| 테스트 | 무엇을 확인하나요? |
|---|---|
| `StarterApplicationTests` | 앱 전체가 뜨는지, `app.name` 설정이 읽히는지 |
| `common/GlobalExceptionHandlerTest` | 예외가 404/400 응답으로 바뀌는지 |
| `sample/SampleServiceTest` | 업무 로직 (Repository를 가짜로 바꿔서 검사) |
| `sample/SampleRepositoryTest` | 실제 H2 DB에 저장/조회가 되는지 |
| `sample/SampleControllerTest` | HTTP 요청/응답 모양, 상태 코드, 검증 실패 |

- 결과 보기: `build/reports/tests/test/index.html` 을 브라우저로 열면 자세한 내용이 나옵니다.
- 테스트가 실패하면 콘솔의 `FAILED` 줄과 `expected ... but was ...` 메시지를 먼저 읽으세요.

---

## STEP 6. 자주 나는 오류

| 증상 | 원인과 해결 |
|---|---|
| `Unsupported class file major version` / `release version 25 not supported` | JDK가 25가 아닙니다. `java -version` 을 확인하고 Amazon Corretto 25를 설치하세요. |
| `Could not find org.springframework.boot:...` | 인터넷 연결 또는 프록시 문제입니다. Maven Central 접속을 확인하세요. |
| `package org.springframework.boot.webmvc.test.autoconfigure does not exist` | 라이브러리 패키지 경로가 다를 수 있습니다. 공식 문서 "Spring Boot 4.0 Migration Guide"에서 `@WebMvcTest`, `@DataJpaTest` 의 실제 import 경로를 확인하고 해당 테스트 파일의 import만 고치세요. |
| `Port 8080 was already in use` | 다른 프로그램이 8080을 쓰고 있습니다. 해당 프로그램을 끄거나 `application.yml`에 `server.port: 8081` 을 추가하세요. |
| 테스트에서 `Application context failed to load` | `application.yml` 들여쓰기(탭 금지, 스페이스 2칸) 또는 DB 설정을 확인하세요. |
| 한국어가 `???` 또는 깨져 보임 | 파일 인코딩이 UTF-8인지 확인하세요. (`build.gradle` 에 `options.encoding = 'UTF-8'` 설정이 이미 있습니다) |

---

## STEP 7. 패키지 이름 바꾸기 (선택)

기본 패키지는 `com.example.starter` 입니다. 내 프로젝트 이름으로 바꾸려면 아래 순서를 따르세요.

1. `src/main/java/com/example/starter` 폴더 이름을 바꿉니다. (예: `com/example/myapp`)
2. `src/test/java/com/example/starter` 폴더도 같은 방식으로 바꿉니다.
3. 모든 `.java` 파일 첫 줄의 `package com.example.starter...` 와, `import com.example.starter...` 를 새 이름으로 바꿉니다.
   - IDE(IntelliJ, VS Code)의 "전체 찾아 바꾸기"를 쓰면 편합니다. 검색어: `com.example.starter`
4. `build.gradle` 의 `group = 'com.example'` 도 필요하면 바꿉니다.
5. `./gradlew test` 로 다시 통과하는지 확인합니다.

---

## STEP 8. DB 바꾸기 (H2 → MySQL, 선택)

H2는 실습용입니다. 실제 DB를 쓰려면 아래 순서를 따르세요.

1. `build.gradle` 에서 `runtimeOnly 'com.h2database:h2'` 를 지우고, 다음을 추가합니다.
   ```groovy
   runtimeOnly 'com.mysql:mysql-connector-j'
   ```
2. `application.yml` 의 `spring.datasource` 를 MySQL 주소로 바꿉니다. **비밀번호는 파일에 직접 적지 말고 환경변수로 받습니다.** (파일이 Git에 올라갈 때 비밀번호가 함께 노출되는 것을 막기 위해서입니다)
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/starter
       username: ${DB_USERNAME}
       password: ${DB_PASSWORD}
   ```
   실행 전에 터미널에서 환경변수를 설정합니다. (Windows PowerShell은 `$env:DB_PASSWORD = "..."`)
   ```bash
   export DB_USERNAME=내아이디
   export DB_PASSWORD=내비밀번호
   ./gradlew bootRun
   ```
3. `ddl-auto: create-drop` 은 실제 데이터를 지우므로 `validate` 또는 `update` 로 바꾸는 것을 권장합니다.
4. 테스트의 `SampleRepositoryTest` 는 `@DataJpaTest` 가 기본적으로 내장 DB로 바꿔서 실행합니다. 그대로 두면 됩니다.

---

## STEP 9. 새 기능을 추가하는 순서

예: `Member` 기능을 추가할 때 `sample` 과 같은 방식으로 파일을 만듭니다.

1. `member/MemberEntity.java` : 테이블 모양 (`@Entity`)
2. `member/MemberRepository.java` : `JpaRepository<MemberEntity, Long>` 를 상속
3. `member/MemberService.java` : 업무 로직, `@Service`, 생성자 주입
4. `member/MemberController.java` : `@RestController`, 요청/응답 record 사용
5. `src/test/java/.../member/` 에 Service 단위 테스트와 Controller 테스트 추가
6. 에러가 필요하면 `ErrorCode` 에 한 줄 추가 후 `BusinessException(ErrorCode.XXX)` 를 던집니다.

---

## 참고: 주석 읽는 법

- 모든 클래스와 메서드 주석에는 **"누가 호출하나요?"**, **"왜 필요한가요?"** 가 적혀 있습니다.
- `@` 로 시작하는 애노테이션 옆 주석은 그 기능이 무엇인지 한 줄로 설명합니다.
- 처음 읽을 때는 `SampleController → SampleService → SampleRepository` 순서로 따라가면 흐름이 잘 보입니다.
