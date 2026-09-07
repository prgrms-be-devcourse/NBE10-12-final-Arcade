package com.back.domain.member.profile.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 request, response 보내는 모양이 달라지지 않아 통일함
 */
public record CareerCommand(
        @Schema(description = "시작 시점", example = "2024-03-01")
        LocalDate startDate,

        @Schema(description = "종료 시점. 생략하면 재직중으로 본다", example = "2025-02-28")
        LocalDate endDate,

        @Schema(description = "[필수] 역할. 비어 있으면 저장하지 않는다", example = "백엔드 엔지니어")
        String role,

        @Schema(description = "회사", example = "그리드모먼트")
        String org,

        @Schema(description = "한 줄 설명", example = "결제·정산 도메인 API 설계")
        String description
) { }
