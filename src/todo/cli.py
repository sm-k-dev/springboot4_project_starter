from __future__ import annotations

import argparse
import re
import sys
from typing import Optional

from todo import service
from todo.service import EmptyTitle, TaskNotFound
from todo.storage import StorageError, default_path, load_tasks, save_tasks
from todo.task import Task

MSG_NOT_FOUND_HINT = "힌트: `todo list`로 id를 확인하세요."
MSG_EMPTY_LIST = "할 일이 없습니다. `todo add <내용>`으로 추가해 보세요."
MSG_READ_HINT = "파일은 수정하지 않았습니다. 내용을 확인하거나 백업해 두세요."
MSG_WRITE_HINT = "변경 내용은 저장되지 않았습니다. 파일 권한을 확인하세요."


_ID_PATTERN = re.compile(r"[0-9]+")


def _parse_id(value: str) -> int:
    # int()는 " 3 ", "+3", "1_0"도 받으므로 ASCII 숫자만 허용한다.
    if not _ID_PATTERN.fullmatch(value):
        raise argparse.ArgumentTypeError("id는 숫자여야 합니다 (예: todo done 3)")
    return int(value)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="todo", description="할 일 관리 CLI")
    sub = parser.add_subparsers(dest="command", metavar="명령")
    sub.required = True

    add = sub.add_parser("add", help="할 일 추가")
    add.add_argument("words", nargs="*", metavar="내용")

    sub.add_parser("list", help="할 일 목록 보기")

    done = sub.add_parser("done", help="할 일 완료 처리")
    done.add_argument("id", type=_parse_id, metavar="id")

    return parser


def _format_list(tasks: list[Task]) -> str:
    width = len(str(max(t.id for t in tasks)))
    lines = []
    for task in tasks:
        mark = "x" if task.done else " "
        lines.append(f"#{task.id:<{width}} [{mark}] {task.title}")
    return "\n".join(lines)


def _run(args: argparse.Namespace) -> int:
    path = default_path()
    tasks = load_tasks(path)

    if args.command == "list":
        if not tasks:
            print(MSG_EMPTY_LIST)
        else:
            print(_format_list(sorted(tasks, key=lambda t: t.id)))
        return 0

    if args.command == "add":
        tasks, task = service.add_task(tasks, " ".join(args.words))
        save_tasks(path, tasks)
        print(f"추가됨: #{task.id} {task.title}")
        return 0

    if args.command == "done":
        tasks, task, changed = service.complete_task(tasks, args.id)
        if changed:
            save_tasks(path, tasks)
            print(f"완료: #{task.id} {task.title}")
        else:
            print(f"이미 완료된 항목입니다: #{task.id} {task.title}")
        return 0

    raise AssertionError(f"알 수 없는 명령: {args.command}")


def main(argv: Optional[list[str]] = None) -> int:
    parser = build_parser()
    try:
        args = parser.parse_args(argv)
    except SystemExit as exc:
        # 사용법 오류(2)와 --help(0)는 argparse가 종료하므로 코드만 돌려준다.
        return int(exc.code or 0)

    try:
        return _run(args)
    except EmptyTitle:
        print("오류: 할 일 내용이 비어 있습니다.", file=sys.stderr)
        return 1
    except TaskNotFound as exc:
        print(f"오류: #{exc.task_id} 항목을 찾을 수 없습니다.", file=sys.stderr)
        print(MSG_NOT_FOUND_HINT, file=sys.stderr)
        return 1
    except StorageError as exc:
        if exc.operation == "read":
            print(f"오류: 할 일 파일을 읽을 수 없습니다: {exc.path}", file=sys.stderr)
            print(MSG_READ_HINT, file=sys.stderr)
        else:
            print(f"오류: 할 일 파일을 저장할 수 없습니다: {exc.path}", file=sys.stderr)
            print(MSG_WRITE_HINT, file=sys.stderr)
        return 1
