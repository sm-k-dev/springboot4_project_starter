from __future__ import annotations

import json
from pathlib import Path

import pytest

from todo import storage
from todo.storage import StorageError, load_tasks, save_tasks
from todo.task import Task


def test_missing_file_gives_empty_list(tmp_path: Path):
    assert load_tasks(tmp_path / "none.json") == []


def test_save_then_load_matches(tmp_path: Path):
    path = tmp_path / "todo.json"
    tasks = [Task(1, "우유 사기", False), Task(2, "보고서 제출", True)]
    save_tasks(path, tasks)
    assert load_tasks(path) == tasks


def test_saved_file_format(tmp_path: Path):
    path = tmp_path / "todo.json"
    save_tasks(path, [Task(1, "우유 사기", False)])
    data = json.loads(path.read_text(encoding="utf-8"))
    assert data == {"version": 1, "tasks": [{"id": 1, "title": "우유 사기", "done": False}]}
    assert "우유 사기" in path.read_text(encoding="utf-8")  # ensure_ascii=False


def test_corrupt_json_raises_and_keeps_file(tmp_path: Path):
    path = tmp_path / "todo.json"
    path.write_text("{ broken", encoding="utf-8")
    with pytest.raises(StorageError) as exc:
        load_tasks(path)
    assert exc.value.operation == "read"
    assert path.read_text(encoding="utf-8") == "{ broken"


@pytest.mark.parametrize(
    "content",
    [
        "[]",
        '{"version": 2, "tasks": []}',
        '{"version": 1, "tasks": "x"}',
        '{"version": 1, "tasks": [{"id": 1, "title": "", "done": false}]}',
    ],
)
def test_wrong_structure_raises(tmp_path: Path, content: str):
    path = tmp_path / "todo.json"
    path.write_text(content, encoding="utf-8")
    with pytest.raises(StorageError):
        load_tasks(path)


def test_atomic_write_leaves_no_tmp_file(tmp_path: Path):
    path = tmp_path / "todo.json"
    save_tasks(path, [Task(1, "a", False)])
    assert not (tmp_path / "todo.json.tmp").exists()


def test_write_failure_raises_storage_error(tmp_path: Path):
    path = tmp_path / "missing_dir" / "todo.json"
    with pytest.raises(StorageError) as exc:
        save_tasks(path, [Task(1, "a", False)])
    assert exc.value.operation == "write"


def test_default_path_uses_env(monkeypatch, tmp_path: Path):
    target = tmp_path / "custom.json"
    monkeypatch.setenv("TODO_FILE", str(target))
    assert storage.default_path() == target


def test_default_path_falls_back_to_home(monkeypatch, tmp_path: Path):
    monkeypatch.delenv("TODO_FILE", raising=False)
    monkeypatch.setattr(Path, "home", classmethod(lambda cls: tmp_path))
    assert storage.default_path() == tmp_path / ".todo.json"


def test_unencodable_text_raises_write_error_and_keeps_original(tmp_path: Path):
    path = tmp_path / "todo.json"
    save_tasks(path, [Task(1, "원본", False)])
    original = path.read_bytes()

    with pytest.raises(StorageError) as exc:
        save_tasks(path, [Task(1, "깨진\udcff글자", False)])

    assert exc.value.operation == "write"
    assert not (tmp_path / "todo.json.tmp").exists()
    assert path.read_bytes() == original


@pytest.mark.parametrize("version", ["true", "1.0", '"1"'])
def test_non_int_version_is_rejected(tmp_path: Path, version: str):
    path = tmp_path / "todo.json"
    path.write_text('{"version": %s, "tasks": []}' % version, encoding="utf-8")
    with pytest.raises(StorageError):
        load_tasks(path)


def test_duplicate_ids_raise_read_error_and_keep_file(tmp_path: Path):
    path = tmp_path / "todo.json"
    content = (
        '{"version": 1, "tasks": ['
        '{"id": 1, "title": "a", "done": false},'
        '{"id": 1, "title": "b", "done": false}]}'
    )
    path.write_text(content, encoding="utf-8")
    with pytest.raises(StorageError) as exc:
        load_tasks(path)
    assert exc.value.operation == "read"
    assert path.read_text(encoding="utf-8") == content
