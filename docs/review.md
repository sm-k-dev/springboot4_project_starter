# 리뷰 결과: 할 일 CLI (서연 구현)

작성: 태양 (리뷰어) · 대상: 민준(판정 수신), 서연(수정 담당) · 기준: `docs/design.md`, `docs/ui.md`

## 판정: 통과 (재검토 완료, 2차)

### 2차 재검토 결과

- 테스트: `python3 -m pytest -q` → **61 passed** (1차 48개 → 61개).
- 반려 1 (인코딩 불가 문자): 해결. `save_tasks`가 `UnicodeEncodeError`를 `StorageError("write")`로 변환하고 `.tmp`를 정리합니다. 재현 시 종료 코드 1, `.tmp` 미잔존, 원본 보존을 확인했습니다. 회귀 테스트: `test_unencodable_text_raises_write_error_and_keeps_original`.
- 반려 2 (중복 id): 해결. `load_tasks`가 중복 id를 `StorageError("read")`로 거부합니다. `list`/`done` 모두 종료 코드 1이며 파일은 변경되지 않았습니다. 회귀 테스트: `test_duplicate_ids_raise_read_error_and_keep_file`, `test_list_with_duplicate_ids_is_exit_one_and_keeps_file`, `test_done_with_duplicate_ids_is_exit_one_and_keeps_file`.
- 권장 사항 1 (`version: true` 제외): 반영. `isinstance(version, int)`와 `bool` 제외로 거부합니다. 테스트: `test_non_int_version_is_rejected`.
- 권장 사항 2 (done id ASCII 숫자만): 반영. `" 3 "`, `"+3"`, `"1_0"`, `"٣"`, `"3.0"`, `""`는 종료 코드 2의 한국어 오류를 냅니다. `"003"`은 3으로 처리됩니다. 테스트: `test_done_rejects_non_plain_digit_ids`.
- 새 결함 없음: 수정 코드(`storage.py`, `cli.py`, `task.py`)를 읽고 재현 스크립트로 확인했습니다. 변경은 요청된 범위 안에 있습니다.

### 1차 판정 기록 (반려, 참고용)

반려 (수정 2건)

## 1. 검토 범위

- 코드: `src/todo/` (task, storage, service, cli, __main__, __init__), `pyproject.toml`
- 테스트: `tests/` 4개 파일 (test_task, test_storage, test_service, test_cli)
- 실행 결과: 프로젝트 루트에서 `python3 -m pytest -q` → **48 passed**

## 2. 설계·UI 준수 확인 (통과 항목)

- `from __future__ import annotations`: 모든 `src/`, `tests/` 파일에 있음.
- 의존 방향 `cli → service → storage/task`, `task.py`는 import 없음: 충족.
- 저장 위치: `TODO_FILE` 우선, 없으면 `~/.todo.json`: 충족.
- 저장 형식: version 1, ensure_ascii=False, indent 2: 충족.
- 쓰기 순서 `.tmp` → flush → fsync → os.replace: 충족(`storage.py:63-69`).
- `load` 실패 시 덮어쓰지 않음(저장 전 `load_tasks`에서 중단): 충족.
- 종료 코드 0/1/2, 오류는 stderr, `오류: ...` 형식: 충족.
- `done` 이미 완료 → 오류 없이 exit 0, 저장하지 않음(`cli.py:68-73`): 충족.
- `list` 자릿수 정렬, 빈 목록 문구(ui.md 3·4장), 한국어 id 오류(ui.md 5.1), 2줄 힌트(ui.md 5장): 문구가 ui.md와 정확히 일치.
- 줄바꿈·탭 → 공백 치환, 앞뒤 공백 제거 후 빈 값 거부: 충족(`service.py:21-29`).
- `pyproject.toml`: `requires-python >=3.9`, `dev = ["pytest>=7"]`, `pythonpath = ["src"]`: 충족.

## 3. 수정 목록

### 수정 1 [필수] 쓰기 중 `UnicodeEncodeError`가 처리되지 않고 임시 파일이 남음

- 위치: `src/todo/storage.py:63-75` (`save_tasks`)
- 현상 (재현 완료):
  - 인자에 잘못된 UTF-8 바이트가 있으면 Python은 `\udcff` 같은 lone surrogate로 받습니다.
  - `todo add "깨진\udcff글자"` → `save_tasks`에서 `UnicodeEncodeError`(ValueError 하위)가 발생합니다.
  - `except OSError`에 잡히지 않으므로 **Traceback이 출력되고 종료 코드 1 규약을 어깁니다.**
  - 임시 파일 `todo.json.tmp`에 일부 내용이 남습니다(정리 코드가 실행되지 않음).
  - 원본 `todo.json`은 보존됨을 확인했습니다.
- 설계 근거: design.md 3장 쓰기 규칙("실패 시 StorageError"), 4장 오류 처리(모든 실행 오류는 `오류:` 형식 + 종료 코드 1).
- 수정 방향: `except OSError`를 `except (OSError, UnicodeEncodeError)` 또는 `except Exception`으로 넓히고, 임시 파일 정리 후 `StorageError("write", path)`로 변환합니다. 입력 단계에서 인코딩 불가 문자를 거부하는 방법도 가능합니다.
- 추가 테스트: 인코딩 불가 문자 저장 시 `StorageError`, 종료 코드 1, `.tmp` 미잔존, 기존 파일 내용 불변을 검증합니다.

### 수정 2 [필수] 중복 id가 들어 있는 파일을 그대로 읽음

- 위치: `src/todo/storage.py:43-52` (`load_tasks`)
- 현상 (재현 완료): `{"id": 1, ...}`가 두 개인 파일에서
  - `list`가 `#1 [ ] a`, `#1 [ ] b`를 모두 출력합니다.
  - `done 1`은 첫 번째 항목만 완료하고 exit 0을 반환합니다. 어느 항목이 대상인지 사용자가 알 수 없습니다.
- 설계 근거: design.md 3장 "id: 1부터 시작, 기존 최대 id + 1"(id 유일성을 전제), 3장 "구조가 맞지 않으면 StorageError".
- 수정 방향: `load_tasks`에서 id 중복을 발견하면 `StorageError("read", path)`를 발생시킵니다.
- 추가 테스트: 중복 id 파일 → `StorageError`, `list`/`done` 모두 종료 코드 1, 파일 불변.

## 4. 권장 사항 (반려 사유 아님, 여유가 있을 때 반영)

- `storage.py:26-40`: `version`에 `True`가 들어오면 `True == 1`이라 통과합니다(`load_tasks` → `[]`). `isinstance(v, int) and not isinstance(v, bool)`로 막으면 더 엄격합니다.
- `cli.py:18-22`: `int()`은 `" 3 "`, `"+3"`, `"1_0"`을 허용합니다. 설계에 명시된 요구는 아니므로 현재 동작을 유지해도 됩니다.
- `storage.py:61`: 임시 파일 이름이 `todo.json.tmp`로 고정입니다. 단일 사용자 범위(design.md 1장)에서는 문제가 없습니다. 공유 폴더에서 쓰는 경우 심볼릭 링크 문제가 생길 수 있으니 참고만 해 주세요.

## 5. 테스트 충분성

- 설계 6장 테스트 계획과 `docs/ui.md` 7장 체크리스트 대부분을 충족합니다(정렬, 빈 내용, 한국어 id 오류, 종료 코드 0/1/2, 손상·저장 실패 문구).
- 빠진 테스트(수정 1·2와 함께 추가 요청):
  - 쓰기 실패 시 `.tmp` 정리와 원본 보존 검증 (현재 성공 경로의 `.tmp` 미잔존만 확인)
  - 중복 id 거부
  - `done`에 없는 id를 넣었을 때 파일이 변경되지 않음을 확인
- 보안 관점: 사용자 입력은 파일 경로로 쓰이지 않고 JSON 값으로만 저장되므로 주입 위험은 확인하지 못했습니다.
- 성능 관점: 항목 수백 건 규모에서 문제가 없습니다(전체 읽기·쓰기 O(n)).
- 가독성 관점: 계층 분리와 함수 역할이 명확합니다. 큰 문제는 없습니다.

## 6. 재검토 조건

수정 1·2가 반영되고 추가 테스트가 통과하면 태양이 다시 리뷰하여 **통과** 판정을 내립니다. 재검토 때는 `python3 -m pytest -q` 결과와 위 재현 케이스를 다시 확인합니다.
