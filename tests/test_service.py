from __future__ import annotations

import pytest

from todo import service
from todo.service import EmptyTitle, TaskNotFound
from todo.task import Task


def test_next_id_starts_at_one():
    assert service.next_id([]) == 1


def test_next_id_is_max_plus_one():
    assert service.next_id([Task(1, "a"), Task(5, "b")]) == 6


def test_add_task_increments_id_and_does_not_mutate():
    original = [Task(1, "a")]
    new_list, task = service.add_task(original, "우유 사기")
    assert task == Task(2, "우유 사기", False)
    assert new_list[-1] == task
    assert original == [Task(1, "a")]


@pytest.mark.parametrize("title", ["", "   ", "\t", "\n"])
def test_add_task_rejects_empty(title: str):
    with pytest.raises(EmptyTitle):
        service.add_task([], title)


def test_add_task_strips_outer_whitespace():
    _, task = service.add_task([], "  우유 사기  ")
    assert task.title == "우유 사기"


def test_add_task_turns_line_breaks_and_tabs_into_space():
    _, task = service.add_task([], "우유\n사기\r\n와\t빵")
    assert task.title == "우유 사기 와 빵"


def test_complete_task_marks_done():
    tasks = [Task(1, "a", False), Task(2, "b", False)]
    new_tasks, task, changed = service.complete_task(tasks, 2)
    assert changed is True
    assert task == Task(2, "b", True)
    assert new_tasks == [Task(1, "a", False), Task(2, "b", True)]
    assert tasks[1].done is False


def test_complete_already_done_returns_false():
    tasks = [Task(1, "a", True)]
    new_tasks, task, changed = service.complete_task(tasks, 1)
    assert changed is False
    assert task.done is True
    assert new_tasks == tasks


def test_complete_missing_id_raises():
    with pytest.raises(TaskNotFound) as exc:
        service.complete_task([Task(1, "a")], 9)
    assert exc.value.task_id == 9
