package com.back.domain.showcase.showcase.dtos;

import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.member.member.entity.PositionType;

import java.time.LocalDateTime;

public record ShowcaseGoalDto(
        long id,
        PartySummary party,
        GoalType type,
        GoalStatus status,
        GoalSource source,
        Detail detail,
        /** 전시된 프로젝트에서 내가 맡았던 포지션. PROJECT 가 아니면 null */
        PositionType positionType,
        int likeCount,
        /** PROJECT 는 원본 파티에 합산된 조회수를 쓴다(기획서 3.2). 자기신고 성취는 원천이 없어 0 */
        int viewCount,
        LocalDateTime createAt
) {
    public record PartySummary(
            long id,
            String name
    ) {
    }

    public record Detail(
            String title
    ) {
    }
}
