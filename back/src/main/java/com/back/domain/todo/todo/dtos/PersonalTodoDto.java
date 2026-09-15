package com.back.domain.todo.todo.dtos;

import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.TodoCategory;
import com.back.domain.todo.todo.entity.TodoStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/** 목록·등록·수정 응답. 항목은 담지 않고 진행률만 준다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalTodoDto(
        long id,
        long ownerId,
        String title,
        TodoCategory category,
        String memo,
        TodoStatus status,
        long totalCount,
        long doneCount,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
    public PersonalTodoDto(PersonalTodo todo, long totalCount, long doneCount) {
        this(
                todo.getId(),
                todo.getOwner().getId(),
                todo.getTitle(),
                todo.getCategory(),
                todo.getMemo(),
                todo.getStatus(),
                totalCount,
                doneCount,
                todo.getCreateDate(),
                todo.getModifyDate()
        );
    }
}
