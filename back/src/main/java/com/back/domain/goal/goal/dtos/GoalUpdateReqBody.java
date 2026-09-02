package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.GoalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 성취 수정 요청.
 *
 * type 은 받지 않는다 - 수상 기록을 체크리스트로 바꾸는 건 고치는 게 아니라 다른 성취를 만드는 일이다.
 *
 * 두 필드 모두 선택이며 넘기지 않으면 그 부분은 건드리지 않는다.
 * 다만 detail 을 넘겼다면 <b>그 타입의 세부 정보는 통째로 교체된다</b> - 화면이 폼 전체를 보내오기 때문에,
 * 안 넘어온 항목은 "그대로 두기"가 아니라 "비우기"로 처리한다.
 */
public record GoalUpdateReqBody(
        @Schema(
                description = "바꿀 진행 상태. 생략하면 상태는 그대로 두고 내용만 고친다",
                example = "ACHIEVED"
        )
        GoalStatus status,

        @Schema(description = "바꿀 세부 정보. 생략하면 내용은 그대로 두고 상태만 바꾼다")
        GoalDetailReqBody detail
) { }
