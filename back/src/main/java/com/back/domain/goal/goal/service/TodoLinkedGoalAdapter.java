package com.back.domain.goal.goal.service;

import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.todo.todo.repository.TodoLinkedGoalPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** TodoLinkedGoalPort 구현. TODO 도메인이 성취를 직접 알지 않게 한다. */
@Component
@RequiredArgsConstructor
public class TodoLinkedGoalAdapter implements TodoLinkedGoalPort {

    private final GoalRepository goalRepository;

    @Override
    public void detachFromTodo(long todoId) {
        goalRepository.findChecklistByPersonalTodoId(todoId)
                .ifPresent(checklist -> {
                    checklist.detachTodo();
                    // FK를 끊는 UPDATE가 TODO의 DELETE보다 먼저 나가야 한다
                    goalRepository.flush();
                });
    }
}
