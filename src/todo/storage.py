from __future__ import annotations

import json
import os
from pathlib import Path

from todo.task import Task

FORMAT_VERSION = 1
DEFAULT_FILENAME = ".todo.json"


class StorageError(Exception):
    """저장 파일을 읽거나 쓰지 못했을 때 발생한다.

    operation: "read" 또는 "write"
    """

    def __init__(self, operation: str, path: Path) -> None:
        self.operation = operation
        self.path = path
        super().__init__(f"{operation} 실패: {path}")


def default_path() -> Path:
    env = os.environ.get("TODO_FILE")
    if env:
        return Path(env)
    return Path.home() / DEFAULT_FILENAME


def load_tasks(path: Path) -> list[Task]:
    """파일이 없으면 빈 목록을 돌려준다. 손상되었으면 StorageError."""
    if not path.exists():
        return []

    try:
        raw = path.read_text(encoding="utf-8")
        data = json.loads(raw)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise StorageError("read", path) from exc

    if not isinstance(data, dict):
        raise StorageError("read", path)
    version = data.get("version")
    if not isinstance(version, int) or isinstance(version, bool) or version != FORMAT_VERSION:
        raise StorageError("read", path)
    items = data.get("tasks")
    if not isinstance(items, list):
        raise StorageError("read", path)

    try:
        tasks = [Task.from_dict(item) for item in items]
    except ValueError as exc:
        raise StorageError("read", path) from exc

    # id는 저장 파일에서 유일해야 한다(design.md 3장). 중복이면 구조 오류로 본다.
    seen: set[int] = set()
    for task in tasks:
        if task.id in seen:
            raise StorageError("read", path)
        seen.add(task.id)
    return tasks


def save_tasks(path: Path, tasks: list[Task]) -> None:
    """같은 폴더의 임시 파일에 쓴 뒤 교체한다(원자적 쓰기)."""
    payload = {
        "version": FORMAT_VERSION,
        "tasks": [task.to_dict() for task in tasks],
    }
    tmp_path = path.with_name(path.name + ".tmp")

    try:
        with open(tmp_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)
            f.write("\n")
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp_path, path)
    except (OSError, UnicodeEncodeError) as exc:
        # with 블록을 빠져나온 뒤이므로 파일은 이미 닫혀 있다.
        try:
            tmp_path.unlink()
        except OSError:
            pass
        raise StorageError("write", path) from exc
