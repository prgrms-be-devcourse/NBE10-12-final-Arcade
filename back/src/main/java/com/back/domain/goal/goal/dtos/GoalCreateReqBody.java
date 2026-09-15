package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record GoalCreateReqBody(
        @Schema(
                description = "성취 유형. PROJECT는 파티 확정 시 자동 생성되는 전용 타입이라 등록할 수 없다",
                allowableValues = {"CONTEST", "CHECKLIST"},
                example = "CONTEST"
        )
        @NotNull GoalType type,

        @Schema(
                description = "진행 상태. 이미 끝난 활동은 WANT를 거치지 않고 바로 ACHIEVED로 등록할 수 있다",
                example = "ACHIEVED"
        )
        @NotNull GoalStatus status,

        @NotNull GoalDetailReqBody detail
) { }
