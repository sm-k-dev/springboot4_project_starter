from __future__ import annotations

from pathlib import Path

import pytest

from todo.cli import main


@pytest.fixture
def todo_file(tmp_path: Path, monkeypatch) -> Path:
    path = tmp_path / "todo.json"
    monkeypatch.setenv("TODO_FILE", str(path))
    return path


def run(capsys, *argv: str):
    code = main(list(argv))
    out, err = capsys.readouterr()
    return code, out, err


def test_add_list_done_list_flow(todo_file, capsys):
    assert run(capsys, "add", "우유", "사기") == (0, "추가됨: #1 우유 사기\n", "")
    assert run(capsys, "add", "보고서 제출") == (0, "추가됨: #2 보고서 제출\n", "")
    assert run(capsys, "list") == (0, "#1 [ ] 우유 사기\n#2 [ ] 보고서 제출\n", "")
    assert run(capsys, "done", "2") == (0, "완료: #2 보고서 제출\n", "")
    assert run(capsys, "list") == (0, "#1 [ ] 우유 사기\n#2 [x] 보고서 제출\n", "")


def test_done_twice_is_ok_and_reports_already_done(todo_file, capsys):
    run(capsys, "add", "우유 사기")
    run(capsys, "done", "1")
    assert run(capsys, "done", "1") == (0, "이미 완료된 항목입니다: #1 우유 사기\n", "")


def test_empty_list_message(todo_file, capsys):
    assert run(capsys, "list") == (
        0,
        "할 일이 없습니다. `todo add <내용>`으로 추가해 보세요.\n",
        "",
    )


def test_list_aligns_status_column_with_two_digit_ids(todo_file, capsys):
    for i in range(10):
        run(capsys, "add", f"항목{i}")
    _, out, _ = run(capsys, "list")
    lines = out.splitlines()
    assert lines[0] == "#1  [ ] 항목0"
    assert lines[8] == "#9  [ ] 항목8"
    assert lines[9] == "#10 [ ] 항목9"
    assert lines[0].index("[") == lines[9].index("[")


def test_list_single_digit_ids_format(todo_file, capsys):
    run(capsys, "add", "a")
    _, out, _ = run(capsys, "list")
    assert out == "#1 [ ] a\n"


def test_add_without_args_is_exit_one(todo_file, capsys):
    code, out, err = run(capsys, "add")
    assert code == 1
    assert out == ""
    assert err == "오류: 할 일 내용이 비어 있습니다.\n"
    assert not todo_file.exists()


def test_add_whitespace_only_is_exit_one(todo_file, capsys):
    code, _, err = run(capsys, "add", "   ")
    assert code == 1
    assert err == "오류: 할 일 내용이 비어 있습니다.\n"


def test_done_missing_id_is_exit_one_with_hint(todo_file, capsys):
    run(capsys, "add", "a")
    code, out, err = run(capsys, "done", "9")
    assert code == 1
    assert out == ""
    assert err == "오류: #9 항목을 찾을 수 없습니다.\n힌트: `todo list`로 id를 확인하세요.\n"


def test_done_non_numeric_id_is_usage_error_in_korean(todo_file, capsys):
    code, _, err = run(capsys, "done", "abc")
    assert code == 2
    assert "usage:" in err
    assert "id는 숫자여야 합니다 (예: todo done 3)" in err


def test_missing_subcommand_is_usage_error(todo_file, capsys):
    code, _, err = run(capsys)
    assert code == 2
    assert "usage:" in err


def test_unknown_command_is_usage_error(todo_file, capsys):
    code, _, _ = run(capsys, "delete", "1")
    assert code == 2


def test_help_exits_zero(todo_file, capsys):
    code, out, _ = run(capsys, "--help")
    assert code == 0
    assert "usage:" in out


def test_corrupt_file_read_error(todo_file, capsys):
    todo_file.write_text("{ broken", encoding="utf-8")
    code, out, err = run(capsys, "list")
    assert code == 1
    assert out == ""
    assert err == (
        f"오류: 할 일 파일을 읽을 수 없습니다: {todo_file}\n"
        "파일은 수정하지 않았습니다. 내용을 확인하거나 백업해 두세요.\n"
    )
    assert todo_file.read_text(encoding="utf-8") == "{ broken"


def test_write_error_message(tmp_path, monkeypatch, capsys):
    path = tmp_path / "no_such_dir" / "todo.json"
    monkeypatch.setenv("TODO_FILE", str(path))
    code, out, err = run(capsys, "add", "a")
    assert code == 1
    assert out == ""
    assert err == (
        f"오류: 할 일 파일을 저장할 수 없습니다: {path}\n"
        "변경 내용은 저장되지 않았습니다. 파일 권한을 확인하세요.\n"
    )


def test_add_unencodable_text_is_exit_one_and_keeps_file(todo_file, capsys):
    run(capsys, "add", "원본")
    original = todo_file.read_bytes()

    code, out, err = run(capsys, "add", "깨진\udcff글자")

    assert code == 1
    assert out == ""
    assert err.startswith("오류: 할 일 파일을 저장할 수 없습니다:")
    assert not todo_file.with_name(todo_file.name + ".tmp").exists()
    assert todo_file.read_bytes() == original


def test_list_with_duplicate_ids_is_exit_one_and_keeps_file(todo_file, capsys):
    content = (
        '{"version": 1, "tasks": ['
        '{"id": 1, "title": "a", "done": false},'
        '{"id": 1, "title": "b", "done": false}]}'
    )
    todo_file.write_text(content, encoding="utf-8")

    code, out, err = run(capsys, "list")
    assert code == 1
    assert out == ""
    assert err.startswith("오류: 할 일 파일을 읽을 수 없습니다:")
    assert todo_file.read_text(encoding="utf-8") == content


def test_done_with_duplicate_ids_is_exit_one_and_keeps_file(todo_file, capsys):
    content = (
        '{"version": 1, "tasks": ['
        '{"id": 1, "title": "a", "done": false},'
        '{"id": 1, "title": "b", "done": false}]}'
    )
    todo_file.write_text(content, encoding="utf-8")

    code, out, err = run(capsys, "done", "1")
    assert code == 1
    assert out == ""
    assert todo_file.read_text(encoding="utf-8") == content


@pytest.mark.parametrize("value", [" 3 ", "+3", "1_0", "-1", "３"])
def test_done_rejects_non_plain_digit_ids(todo_file, capsys, value: str):
    code, _, err = run(capsys, "done", value)
    assert code == 2
    assert "id는 숫자여야 합니다 (예: todo done 3)" in err
