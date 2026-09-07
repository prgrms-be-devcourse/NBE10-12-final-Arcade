package com.back.domain.showcase.showcase.dtos;

import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;

import java.time.LocalDateTime;

public record ShowcaseGoalDto(
        long id,
        PartySummary party,
        GoalType type,
        GoalStatus status,
        GoalSource source,
        Detail detail,
        int likeCount,
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
