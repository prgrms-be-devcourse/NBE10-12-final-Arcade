package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 성취 상세 조회 응답.
 *
 * 목록(GoalDto)과 공통 필드는 같고, PROJECT 성취일 때만 파티 정보(project)가 함께 온다.
 * 목록에서는 파티를 조회하지 않으므로 이 필드를 붙이지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalDetailResponseDto(
        long id,
        long ownerId,
        String ownerName,
        GoalType type,
        GoalStatus status,
        GoalSource source,
        Long sourcePartyId,
        int viewCount,
        GoalDetailDto detail,
        /** type 이 PROJECT 일 때만 값이 있다 */
        ProjectContextDto project,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
    public GoalDetailResponseDto(Goal goal, ProjectContextDto project) {
        this(
                goal.getId(),
                goal.getOwner().getId(),
                goal.getOwner().getName(),
                goal.getType(),
                goal.getStatus(),
                goal.getSource(),
                goal.getSourcePartyId(),
                goal.getViewCount(),
                GoalDetailDto.from(goal),
                project,
                goal.getCreateDate(),
                goal.getModifyDate()
        );
    }
}
