package com.back.domain.todo.todo.dtos;

import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.PersonalTodoItem;
import com.back.domain.todo.todo.entity.TodoCategory;
import com.back.domain.todo.todo.entity.TodoStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/** 상세. items는 첫 페이지만 담는다 - 나머지는 GET /todos/{id}/items로 받는다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonalTodoDetailDto(
        long id,
        long ownerId,
        String title,
        TodoCategory category,
        String memo,
        TodoStatus status,
        long totalCount,
        long doneCount,
        List<PersonalTodoItemDto> items,
        boolean hasMoreItems,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
    public PersonalTodoDetailDto(PersonalTodo todo, Page<PersonalTodoItem> firstPage, long doneCount) {
        this(
                todo.getId(),
                todo.getOwner().getId(),
                todo.getTitle(),
                todo.getCategory(),
                todo.getMemo(),
                todo.getStatus(),
                firstPage.getTotalElements(),
                doneCount,
                firstPage.getContent().stream().map(PersonalTodoItemDto::new).toList(),
                firstPage.hasNext(),
                todo.getCreateDate(),
                todo.getModifyDate()
        );
    }
}
