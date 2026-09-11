package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;

import java.time.LocalDateTime;

public record MemberAchievementItemDto(
        long id,
        GoalType type,
        String title,
        GoalStatus status,
        GoalSource source,
        LocalDateTime createDate
) {
    public MemberAchievementItemDto(Goal goal) {
        this(goal.getId(), goal.getType(), goal.getTitle(), goal.getStatus(), goal.getSource(), goal.getCreateDate());
    }
}
