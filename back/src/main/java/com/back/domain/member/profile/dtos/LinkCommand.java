package com.back.domain.member.profile.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/** 링크 한 줄을 바꾸라는 지시. 경력과 마찬가지로 보낸 목록이 곧 저장될 목록이다. */
public record LinkCommand(
        @Schema(description = "[필수] 이름. 자유 입력이다", example = "GitHub")
        String label,

        @Schema(description = "[필수] 주소. 스킴이 없으면 서버가 https 를 붙인다", example = "https://github.com/skyjeong")
        String url
) { }
