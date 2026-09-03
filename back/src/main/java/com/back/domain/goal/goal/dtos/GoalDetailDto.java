package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.member.member.entity.PositionType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

// 타입별 세부 필드를 하나의 detail 객체로 조립한다. 해당 타입에 없는 필드는 응답에서 빠진다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalDetailDto(
        // 세 타입 공용 - PROJECT 는 파티 이름, CONTEST 는 대회명, CHECKLIST 는 목표 제목
        String title,

        // PROJECT + CONTEST 공용
        String result,

        // PROJECT
        PositionType positionType,
        LocalDate startDate,
        LocalDate endDate,

        // CONTEST
        Boolean isTeam,
        LocalDate awardDate,
        String contestUrl,
        Long targetContestId,
        // 증빙자료는 Object Storage 에 두고 여기엔 메타데이터만 내려준다
        String evidenceStorageKey,
        String evidenceFileName,
        String evidenceMimeType,
        Long evidenceSize,

        // CHECKLIST
        String memo,
        LocalDate targetDate
) {
    public static GoalDetailDto from(Goal goal) {
        if (goal instanceof Project project) {
            return new GoalDetailDto(
                    // PROJECT 의 result 는 없앴다 - 전시글 내용은 상세 응답의 project 블록에서 온다
                    project.getTitle(), null,
                    project.getPositionType(), project.getStartDate(), project.getEndDate(),
                    null, null, null, null, null, null, null, null,
                    null, null
            );
        }

        if (goal instanceof PersonalContest contest) {
            return new GoalDetailDto(
                    contest.getTitle(), contest.getResult(),
                    null, null, null,
                    contest.isTeam(), contest.getAwardDate(),
                    contest.getContestUrl(), contest.getTargetContestId(),
                    contest.getEvidenceStorageKey(), contest.getEvidenceFileName(),
                    contest.getEvidenceMimeType(), contest.getEvidenceSize(),
                    null, null
            );
        }

        if (goal instanceof PersonalChecklist checklist) {
            return new GoalDetailDto(
                    checklist.getTitle(), null,
                    null, null, null,
                    null, null, null, null, null, null, null, null,
                    checklist.getMemo(), checklist.getTargetDate()
            );
        }

        throw new IllegalStateException("알 수 없는 성취 타입입니다: " + goal.getClass());
    }
}
