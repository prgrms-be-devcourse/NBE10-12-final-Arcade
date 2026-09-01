package com.back.domain.goal.goal.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

// 타입별로 쓰는 필드가 달라 detail 하나에 모아 받는다. 실제로 무엇이 필수인지는 타입에 따라 서비스가 검증한다.
@Schema(description = "타입별 세부 정보. type에 해당하지 않는 필드는 무시된다")
public record GoalDetailReqBody(
        @Schema(description = "[CONTEST 필수] 대회명", example = "사내 해커톤")
        String contestName,

        @Schema(description = "[CONTEST] 팀 참가 여부. 생략하면 false(개인)", example = "true")
        Boolean isTeam,

        @Schema(description = "[CONTEST] 수상 결과", example = "장려상")
        String result,

        @Schema(description = "[CONTEST] 수상일. 성취 리스트의 연도 그룹핑 기준", example = "2023-11-15")
        LocalDate awardDate,

        @Schema(description = "[CHECKLIST 필수] 목표 제목", example = "정보처리기사 실기 준비")
        String title,

        @Schema(description = "[CHECKLIST] 메모", example = "매주 토요일 2시간씩")
        String memo,

        @Schema(description = "[CHECKLIST] 시작일 또는 목표일", example = "2026-10-01")
        LocalDate targetDate
) { }
