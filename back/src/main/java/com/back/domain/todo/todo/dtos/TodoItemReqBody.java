package com.back.domain.todo.todo.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 항목 추가·수정 공용. 화면이 내용만 보낸다. */
public record TodoItemReqBody(
        @Schema(description = "할 일 내용", example = "기출 3회분 풀이")
        @NotBlank String content
) { }
