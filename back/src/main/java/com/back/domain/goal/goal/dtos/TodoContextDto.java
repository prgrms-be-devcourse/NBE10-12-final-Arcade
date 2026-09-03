package com.back.domain.goal.goal.dtos;

import com.back.domain.todo.todo.dtos.PersonalTodoItemDto;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.PersonalTodoItem;
import com.back.domain.todo.todo.entity.TodoCategory;
import com.back.domain.todo.todo.entity.TodoStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * CHECKLIST 성취에 연결된 개인 TODO. 상세 화면이 이걸로 진행 과정을 그린다.
 *
 * 개인 TODO 자체는 비공개(/todos 는 소유자만)지만 이 블록은 남의 성취에서도 채운다 -
 * 성취에 연결하는 행위를 진행 과정을 공개하겠다는 뜻으로 본다.
 * 다만 화면은 여기서 개인 TODO 페이지로 이동시키면 안 된다. 그 화면은 소유자 전용이라 403 이 난다.
 *
 * items 는 완료한 항목만, 해낸 순서대로 담는다. 미완료 항목은 아직 못 한 일이라 내보내지 않는다.
 * 진행률도 담지 않는다 - 남은 개수를 유추할 수 있으면 안 감춘 것과 같다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TodoContextDto(
        long todoId,
        String title,
        TodoCategory category,
        String memo,
        TodoStatus status,
        /** 완료한 항목만, 완료 시각 순. 개인 TODO 항목은 많아야 수십 개라 끊지 않는다 */
        List<PersonalTodoItemDto> items
) {
    public static TodoContextDto of(PersonalTodo todo, List<PersonalTodoItem> items) {
        return new TodoContextDto(
                todo.getId(),
                todo.getTitle(),
                todo.getCategory(),
                todo.getMemo(),
                todo.getStatus(),
                items.stream().map(PersonalTodoItemDto::new).toList()
        );
    }
}
