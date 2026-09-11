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
        GoalDetailDto detail,
        /** type 이 PROJECT 일 때만 값이 있다 */
        ProjectContextDto project,
        /** CHECKLIST 성취에 개인 TODO 가 연결됐을 때만 값이 있다 */
        TodoContextDto todo,
        LocalDateTime createDate,
        LocalDateTime modifyDate
) {
    /** owner - 지금 보고 있는 사람이 이 성취의 주인인지. 본인에게만 보일 값(증빙 반려 사유)을 가른다 */
    public GoalDetailResponseDto(Goal goal, ProjectContextDto project, TodoContextDto todo, boolean owner) {
        this(
                goal.getId(),
                goal.getOwner().getId(),
                goal.getOwner().getName(),
                goal.getType(),
                goal.getStatus(),
                goal.getSource(),
                goal.getSourcePartyId(),
                GoalDetailDto.from(goal, owner),
                project,
                todo,
                goal.getCreateDate(),
                goal.getModifyDate()
        );
    }
}
