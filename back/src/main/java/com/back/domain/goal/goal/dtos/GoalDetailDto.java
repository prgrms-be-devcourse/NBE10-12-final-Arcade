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
        // PROJECT + CHECKLIST 공용
        String title,

        // PROJECT + CONTEST 공용
        String result,

        // PROJECT
        PositionType positionType,
        LocalDate startDate,
        LocalDate endDate,

        // CONTEST
        String contestName,
        Boolean isTeam,
        LocalDate awardDate,
        Long targetContestId,

        // CHECKLIST
        String memo,
        LocalDate targetDate
) {
    public static GoalDetailDto from(Goal goal) {
        if (goal instanceof Project project) {
            return new GoalDetailDto(
                    project.getTitle(), project.getResult(),
                    project.getPositionType(), project.getStartDate(), project.getEndDate(),
                    null, null, null, null,
                    null, null
            );
        }

        if (goal instanceof PersonalContest contest) {
            return new GoalDetailDto(
                    null, contest.getResult(),
                    null, null, null,
                    contest.getContestName(), contest.isTeam(), contest.getAwardDate(), contest.getTargetContestId(),
                    null, null
            );
        }

        if (goal instanceof PersonalChecklist checklist) {
            return new GoalDetailDto(
                    checklist.getTitle(), null,
                    null, null, null,
                    null, null, null, null,
                    checklist.getMemo(), checklist.getTargetDate()
            );
        }

        throw new IllegalStateException("알 수 없는 성취 타입입니다: " + goal.getClass());
    }
}
