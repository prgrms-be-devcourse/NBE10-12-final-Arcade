package com.back.domain.todo.todo.dtos;

import com.back.domain.todo.todo.entity.TodoCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TodoCreateReqBody(
        @Schema(description = "제목", example = "정보처리기사 실기 준비")
        @NotBlank String title,

        @Schema(description = "분류", example = "STUDY")
        @NotNull TodoCategory category,

        @Schema(description = "메모", example = "매주 토요일 2시간씩")
        String memo
) { }
