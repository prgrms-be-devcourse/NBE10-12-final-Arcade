package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;

import java.time.LocalDateTime;

public record GoalDto(
        long id,
        long ownerId,
        GoalType type,
        GoalStatus status,
        GoalSource source,
        Long sourcePartyId,
        Long partyAssembleToMemberId,
        int viewCount,
        GoalDetailDto detail,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
    public GoalDto(Goal goal) {
        this(
                goal.getId(),
                goal.getOwner().getId(),
                goal.getType(),
                goal.getStatus(),
                goal.getSource(),
                goal.getSourcePartyId(),
                goal.getPartyAssembleToMemberId(),
                goal.getViewCount(),
                GoalDetailDto.from(goal),
                goal.getCreateDate(),
                goal.getModifyDate()
        );
    }
}
