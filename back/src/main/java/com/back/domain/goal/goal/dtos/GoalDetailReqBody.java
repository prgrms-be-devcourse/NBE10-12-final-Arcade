package com.back.domain.goal.goal.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

// 타입별로 쓰는 필드가 달라 detail 하나에 모아 받는다. 실제로 무엇이 필수인지는 타입에 따라 서비스가 검증한다.
@Schema(description = "타입별 세부 정보. type에 해당하지 않는 필드는 무시된다")
public record GoalDetailReqBody(
        @Schema(description = "[CONTEST] 팀 참가 여부. 생략하면 false(개인)", example = "true")
        Boolean isTeam,

        @Schema(description = "[CONTEST] 수상 결과", example = "장려상")
        String result,

        @Schema(description = "[CONTEST] 수상일. 성취 리스트의 연도 그룹핑 기준", example = "2023-11-15")
        LocalDate awardDate,

        @Schema(
                description = "[CONTEST] 외부 대회의 공고·결과 발표 페이지 주소. 자기신고는 크루온에 없는 대회를 적는 것이라 링크가 유일한 근거다",
                example = "https://example.com/contest"
        )
        String contestUrl,

        @Schema(
                description = "[CONTEST] 증빙 파일명. 파일 업로드 API가 아직 없어 메타데이터만 기록한다",
                example = "수상확인서.pdf"
        )
        String evidenceFileName,

        @Schema(description = "[CONTEST] 증빙 파일 형식", example = "application/pdf")
        String evidenceMimeType,

        @Schema(description = "[CONTEST] 증빙 파일 크기(byte)", example = "204800")
        Long evidenceSize,

        @Schema(
                description = "[필수] 성취 이름. CONTEST 는 대회명, CHECKLIST 는 목표 제목이다",
                example = "정보처리기사 실기 준비"
        )
        String title,

        @Schema(description = "[CHECKLIST] 메모", example = "매주 토요일 2시간씩")
        String memo,

        @Schema(description = "[CHECKLIST] 시작일 또는 목표일", example = "2026-10-01")
        LocalDate targetDate
) { }
