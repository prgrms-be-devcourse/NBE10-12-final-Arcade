package com.back.domain.todo.todo.dtos;

import com.back.domain.todo.todo.entity.PersonalTodoItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalTodoItemDto(
        long id,
        String content,
        boolean done,
        /** 체크한 시각. 성취 상세의 발자취 표기에 쓴다 */
        LocalDateTime doneAt,
        int sortOrder
) {
    public PersonalTodoItemDto(PersonalTodoItem item) {
        this(item.getId(), item.getContent(), item.isDone(), item.getDoneAt(), item.getSortOrder());
    }
}
