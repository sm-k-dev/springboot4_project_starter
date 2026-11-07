# Python 할 일 관리 CLI 설계

작성: 민준 (아키텍트) · 대상: 쭌(팀장), 지훈, 수아, 서연, 태양

## 1. 목표와 범위

할 일을 **추가 / 목록 / 완료** 세 가지 기능만 제공하는 명령줄 도구입니다.

- 범위 밖(이번 버전에서 하지 않음): 삭제, 수정, 마감일, 우선순위, 태그, 검색, 완료 취소, 여러 사용자.
- 외부 의존성 없이 표준 라이브러리만 사용합니다(Python 3.9 이상).
- 테스트만 `pytest>=7`을 사용합니다(개발 의존성).
- 모든 `.py` 파일 상단에 `from __future__ import annotations`를 둡니다. 3.9에서 `X | None` 타입 힌트가 실행 시점에 오류를 내지 않도록 하기 위함입니다.
- 단일 사용자 기준입니다. 두 프로세스가 동시에 `add`/`done`을 실행하면 나중 쓰기가 앞선 변경을 덮어쓸 수 있습니다(파일은 깨지지 않음). 이번 범위에서는 동시 실행을 지원하지 않습니다.

## 2. 폴더 구조와 파일 목록

```
project/
├── CLAUDE.md
├── pyproject.toml              # 서연: 패키지 메타데이터, pytest 설정
├── docs/
│   ├── design.md               # 민준: 이 문서
│   ├── research.md             # 지훈: 저장 방식·라이브러리 조사
│   └── ui.md                   # 수아: 명령별 출력 문구와 화면 흐름
├── src/
│   └── todo/
│       ├── __init__.py         # 서연: 버전 상수만 둠
│       ├── __main__.py         # 서연: `python -m todo` 진입점 (cli.main 호출)
│       ├── task.py             # 서연: Task 데이터 클래스, dict 변환
│       ├── storage.py          # 서연: JSON 파일 읽기/쓰기, 기본 경로 결정
│       ├── service.py          # 서연: add_task / list_tasks / complete_task (순수 로직)
│       └── cli.py              # 서연: argparse 설정, 출력, 종료 코드
└── tests/
    ├── test_task.py            # 서연
    ├── test_storage.py         # 서연
    ├── test_service.py         # 서연
    └── test_cli.py             # 서연
```

의존 방향: `cli → service → storage/task`. `task.py`는 아무것도 import하지 않습니다.

## 3. 데이터 모델

```python
@dataclass
class Task:
    id: int        # 1부터 시작, 기존 최대 id + 1 (삭제가 없으므로 재사용 문제 없음)
    title: str     # 공백 제거 후 비어 있으면 안 됨
    done: bool     # 기본 False
```

저장 파일 형식 (JSON, UTF-8, `ensure_ascii=False`, 들여쓰기 2칸):

```json
{
  "version": 1,
  "tasks": [
    {"id": 1, "title": "우유 사기", "done": false},
    {"id": 2, "title": "보고서 제출", "done": true}
  ]
}
```

### 저장 위치

- 기본값: `~/.todo.json`
- 환경 변수 `TODO_FILE`이 있으면 그 경로를 사용합니다(테스트와 사용자 지정용).
- 파일이 없으면 빈 목록으로 취급하고, 쓸 때 새로 만듭니다.

### 쓰기 규칙

- 임시 파일(`<path>.tmp`, 같은 폴더)에 쓰고 → `flush()` → `os.fsync(f.fileno())` → `os.replace(tmp, path)` 순서로 교체합니다. 같은 폴더에 만들어야 `os.replace`가 원자적으로 동작하고, fsync는 정전이나 강제 종료 시 빈 파일이 남는 것을 막습니다.
- 파일이 JSON으로 읽히지 않거나 구조가 맞지 않으면 `StorageError`를 발생시키고, 파일을 덮어쓰지 않습니다.
- 읽을 때 id가 중복된 항목이 있으면 구조 오류로 보고 `StorageError`를 발생시킵니다(id 유일성은 저장 파일의 불변 조건입니다).
- 쓸 때 인코딩할 수 없는 문자(lone surrogate 등, `UnicodeEncodeError`)가 있으면 `StorageError("write", path)`로 변환합니다. 임시 파일은 정리하고 원본은 그대로 둡니다. 모든 저장 실패 경로는 `OSError`와 `UnicodeEncodeError`를 함께 처리합니다.

## 4. 기능과 명령

| 명령 | 동작 | 성공 출력(기본 문구) | 종료 코드 |
|---|---|---|---|
| `todo add [내용...]` | 새 할 일 추가 (여러 단어는 공백으로 이어 붙임, 인자가 없으면 빈 내용으로 처리) | `추가됨: #3 우유 사기` | 0 |
| `todo list` | 전체 목록 출력 (완료 여부 표시) | 아래 예시 참고 | 0 |
| `todo done <id>` | id 항목을 완료 상태로 변경 | `완료: #3 우유 사기` | 0 |

`list` 출력 예:

```
#1 [ ] 우유 사기
#2 [x] 보고서 제출
```

할 일이 하나도 없으면 `할 일이 없습니다.`를 출력합니다.

### 세부 규칙

- `add`: argparse는 `nargs="*"`로 설정합니다. 인자가 없거나 공백만 있으면 합친 뒤 앞뒤 공백을 제거한 결과가 비어 있으므로 종료 코드 1의 `빈 내용` 오류로 통일합니다(`todo add`만 입력해도 같은 오류). 여러 단어 입력(`todo add 우유 사기`)도 허용합니다.
- `list`: id 오름차순으로 출력합니다. 완료된 항목도 항상 표시합니다(필터 옵션 없음).
- `done`: id가 없으면 오류입니다. 이미 완료된 항목이면 오류 없이 `이미 완료된 항목입니다: #2 보고서 제출`을 출력하고 종료 코드 0을 반환합니다.
- `done`의 id는 정수여야 합니다(argparse `type=int`).

### 출력 문구의 기준 문서

- 실제 출력 문구와 `list` 형식의 **최종 기준은 `docs/ui.md`**(수아)입니다. 아래 4장의 문구는 기본값이며, `ui.md`와 다르면 `ui.md`를 따릅니다.
- 민준 결정 사항(ui.md 확인 요청에 대한 답):
  - `list`는 id 자릿수에 맞춰 상태 열을 정렬합니다(ui.md 3장). 채택.
  - `done`의 id 숫자 오류와 사용법 오류 문구는 한국어로 씁니다(ui.md 5.1). 채택. argparse의 `usage:` 줄은 유지합니다.
  - 내용에 줄바꿈(`\n`, `\r`, `\t`)이 있으면 공백 하나로 바꿉니다. 저장과 출력 모두 한 줄을 유지합니다. 채택.
  - `todo add`를 인자 없이 실행하면 **종료 코드 1**의 `오류: 할 일 내용이 비어 있습니다.`로 처리합니다(4장 기준). ui.md 5장의 종료 코드 2 안은 받지 않습니다. 빈 내용 오류는 하나로 통일합니다. 문구에 `예: todo add 우유 사기` 힌트를 붙여도 됩니다.

### 오류 처리

- 오류 메시지는 **stderr**로 출력합니다. 형식: `오류: <설명>`
- 기본 오류 문구:
  - 빈 내용: `오류: 할 일 내용이 비어 있습니다.`
  - 없는 id: `오류: #9 항목을 찾을 수 없습니다.`
  - 저장 파일 손상: `오류: 할 일 파일을 읽을 수 없습니다: <경로>`
  - 쓰기 실패(권한 등): `오류: 할 일 파일을 저장할 수 없습니다: <경로>`
- 종료 코드:
  - `0`: 성공
  - `1`: 실행 오류(빈 내용, 없는 id, 저장 오류 등)
  - `2`: 사용법 오류(인자 누락, 잘못된 명령 등, argparse 기본 동작)

## 5. 모듈별 책임과 인터페이스

```python
# storage.py
def default_path() -> Path: ...                      # TODO_FILE 또는 ~/.todo.json
def load_tasks(path: Path) -> list[Task]: ...        # 없으면 []
def save_tasks(path: Path, tasks: list[Task]) -> None: ...

# service.py  (파일 I/O 없음: 목록을 받아 목록을 돌려주는 순수 함수)
def add_task(tasks: list[Task], title: str) -> tuple[list[Task], Task]: ...
def complete_task(tasks: list[Task], task_id: int) -> tuple[list[Task], Task, bool]: ...
#   반환 bool: 이번 호출로 실제로 완료 처리되었는지 (이미 완료면 False)
def next_id(tasks: list[Task]) -> int: ...

# cli.py
def main(argv: list[str] | None = None) -> int: ...
#   예외 StorageError / TaskNotFound / EmptyTitle 을 잡아 stderr 출력 후 1 반환
```

예외 클래스는 `service.py`(`EmptyTitle`, `TaskNotFound`)와 `storage.py`(`StorageError`)에 둡니다.

`cli.main`의 흐름: 인자 파싱 → `load_tasks` → `service` 호출 → 변경이 있을 때만 `save_tasks` → 출력. `list`는 저장하지 않습니다.

## 6. 테스트 계획

- `test_task.py`: dict 변환 왕복, 잘못된 dict 거부.
- `test_storage.py`: 파일 없음 → `[]`, 저장 후 다시 읽기 일치, 손상된 JSON → `StorageError`, 원자적 쓰기(임시 파일이 남지 않음).
- `test_service.py`: id 증가, 빈 제목 거부(앞뒤 공백 제거 포함), 없는 id → `TaskNotFound`, 이미 완료된 항목 재완료 시 bool False.
- `test_cli.py`: `TODO_FILE`을 `tmp_path`로 지정하고 `main([...])`을 호출해 `add → list → done → list` 흐름을 확인. 출력 문구(stdout/stderr)와 종료 코드(0/1/2)를 모두 검증.
- 실행 명령: `pytest -q tests/` (프로젝트 루트에서). 실행 경로 설정은 `pyproject.toml`의 `pythonpath = ["src"]`로 해결합니다.

## 7. 팀 작업 분담

| 담당 | 산출물 | 완료 조건 |
|---|---|---|
| 지훈 (리서처) | `docs/research.md` | 저장 방식(JSON 파일 vs SQLite)과 CLI 라이브러리(argparse vs click) 비교와 추천 한 가지. 이번 설계(JSON + argparse)가 타당한지 의견 포함. |
| 수아 (디자이너) | `docs/ui.md` | 3개 명령의 성공·오류 문구 최종안, `list` 출력 형식, 빈 목록 문구. 위 4장의 기본 문구와 다르면 이유를 적고 최종안을 표시. |
| 서연 (개발자) | `src/`, `tests/`, `pyproject.toml` | 5·6장 구조대로 구현, 모든 파일에 `from __future__ import annotations` 적용, `pyproject.toml`에 `requires-python = ">=3.9"`와 `pytest>=7` 명시, `pytest -q tests/` 전부 통과. 문구는 `docs/ui.md`가 있으면 그것을, 없으면 4장을 따름. |
| 태양 (리뷰어) | 리뷰 결과 보고 | 설계 준수, 오류 처리, 테스트 충분성, 파일 저장 안전성 확인 후 통과/반려 판정. |
| 민준 (아키텍트) | 이 문서, 진행 관리 | 서연 구현 완료 후 태양에게 리뷰 요청, 반려 시 서연에게 전달, 통과 시 쭌에게 요약 보고. |

진행 순서:
1. 민준: 설계 문서 작성 (완료)
2. 지훈·수아: 조사 및 UI 문구 작성 (병렬)
3. 서연: 구현 및 테스트 작성 (2 완료 후 `docs/ui.md` 반영, 없으면 4장 기준으로 진행)
4. 태양: 리뷰
5. 민준: 통과 시 쭌에게 보고

## 8. 결정 사항 요약 (근거)

- **JSON 파일 저장**: 할 일 수백 건 규모이고 기능이 3개뿐이라 DB는 과합니다. 사람이 열어볼 수 있습니다. SQLite도 표준 라이브러리이므로 의존성은 이유가 아닙니다. 삭제·검색·다중 사용자가 추가되면 `storage.py`만 바꿔 SQLite로 전환하는 것을 다시 검토합니다.
- **argparse**: 외부 의존성 없이 하위 명령을 처리할 수 있습니다.
- **service 계층 분리**: 파일 I/O 없이 로직을 테스트하기 쉽게 하기 위함입니다.
- **원자적 쓰기**: 쓰는 도중 강제 종료되어도 기존 데이터가 깨지지 않도록 하기 위함입니다.
