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
                // 목록은 공개 프로필에도 그대로 나가므로 본인 전용 값을 빼고 담는다
                GoalDetailDto.from(goal, false),
                goal.getCreateDate(),
                goal.getModifyDate()
        );
    }
}
