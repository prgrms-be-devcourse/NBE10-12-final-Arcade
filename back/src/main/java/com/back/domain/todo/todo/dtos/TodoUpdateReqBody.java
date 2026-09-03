package com.back.domain.todo.todo.dtos;

import com.back.domain.todo.todo.entity.TodoCategory;
import com.back.domain.todo.todo.entity.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 부분 수정. null이면 그대로 두고, 빈 문자열이면 비운다. */
public record TodoUpdateReqBody(
        @Schema(description = "바꿀 제목", example = "정보처리기사 실기 준비")
        String title,

        @Schema(description = "바꿀 분류", example = "CERTIFICATE")
        TodoCategory category,

        @Schema(description = "바꿀 메모. 빈 문자열이면 비운다", example = "실기 2026.10.18")
        String memo,

        @Schema(description = "바꿀 상태", example = "IN_PROGRESS")
        TodoStatus status
) { }
