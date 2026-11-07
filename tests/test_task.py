from __future__ import annotations

import pytest

from todo.task import Task


def test_to_dict_roundtrip():
    task = Task(id=2, title="보고서 제출", done=True)
    assert Task.from_dict(task.to_dict()) == task


def test_from_dict_defaults_match_fields():
    assert Task.from_dict({"id": 1, "title": "우유 사기", "done": False}) == Task(1, "우유 사기", False)


@pytest.mark.parametrize(
    "data",
    [
        "not a dict",
        {"title": "x", "done": False},
        {"id": "1", "title": "x", "done": False},
        {"id": True, "title": "x", "done": False},
        {"id": 0, "title": "x", "done": False},
        {"id": 1, "title": "   ", "done": False},
        {"id": 1, "title": 3, "done": False},
        {"id": 1, "title": "x", "done": "no"},
    ],
)
def test_from_dict_rejects_invalid(data):
    with pytest.raises(ValueError):
        Task.from_dict(data)
