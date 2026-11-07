from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass
class Task:
    id: int
    title: str
    done: bool = False

    def to_dict(self) -> dict[str, Any]:
        return {"id": self.id, "title": self.title, "done": self.done}

    @classmethod
    def from_dict(cls, data: Any) -> Task:
        """dict에서 Task를 만든다. 형식이 맞지 않으면 ValueError."""
        if not isinstance(data, dict):
            raise ValueError("task는 객체여야 합니다")

        task_id = data.get("id")
        title = data.get("title")
        done = data.get("done")

        if not isinstance(task_id, int) or isinstance(task_id, bool) or task_id < 1:
            raise ValueError("id는 1 이상의 정수여야 합니다")
        if not isinstance(title, str) or not title.strip():
            raise ValueError("title은 비어 있지 않은 문자열이어야 합니다")
        if not isinstance(done, bool):
            raise ValueError("done은 true/false여야 합니다")

        return cls(id=task_id, title=title, done=done)
