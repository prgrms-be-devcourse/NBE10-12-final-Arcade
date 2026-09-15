package com.back.domain.todo.todo.dtos;

/** 목록 진행률. 여러 TODO 것을 group by 로 한 번에 모아 온다. */
public record TodoProgressDto(Long todoId, long totalCount, long doneCount) { }
