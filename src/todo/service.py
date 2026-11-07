from __future__ import annotations

import re
from dataclasses import replace

from todo.task import Task


class EmptyTitle(Exception):
    """할 일 내용이 비어 있을 때 발생한다."""


class TaskNotFound(Exception):
    """없는 id를 참조했을 때 발생한다."""

    def __init__(self, task_id: int) -> None:
        self.task_id = task_id
        super().__init__(f"#{task_id} 항목을 찾을 수 없습니다")


_LINE_BREAKS = re.compile(r"[\r\n\t]+")


def normalize_title(title: str) -> str:
    """줄바꿈·탭은 공백 하나로 바꾸고 앞뒤 공백을 제거한다. 결과가 비면 EmptyTitle."""
    cleaned = _LINE_BREAKS.sub(" ", title).strip()
    if not cleaned:
        raise EmptyTitle()
    return cleaned


def next_id(tasks: list[Task]) -> int:
    return max((t.id for t in tasks), default=0) + 1


def add_task(tasks: list[Task], title: str) -> tuple[list[Task], Task]:
    task = Task(id=next_id(tasks), title=normalize_title(title), done=False)
    return [*tasks, task], task


def complete_task(tasks: list[Task], task_id: int) -> tuple[list[Task], Task, bool]:
    """완료 처리한다. 반환 bool은 이번 호출로 실제 변경되었는지 여부."""
    for index, task in enumerate(tasks):
        if task.id != task_id:
            continue
        if task.done:
            return list(tasks), task, False
        updated = replace(task, done=True)
        new_tasks = list(tasks)
        new_tasks[index] = updated
        return new_tasks, updated, True
    raise TaskNotFound(task_id)
