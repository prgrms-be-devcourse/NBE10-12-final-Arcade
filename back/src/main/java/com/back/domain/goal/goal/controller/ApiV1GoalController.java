package com.back.domain.goal.goal.controller;

import com.back.domain.goal.goal.dtos.GoalCreateReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailReqBody;
import com.back.domain.goal.goal.dtos.GoalDto;
import com.back.domain.goal.goal.service.GoalService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "ApiV1GoalController", description = "성취(마일스톤) 컨트롤러")
public class ApiV1GoalController {

    private final GoalService goalService;
    private final Rq rq;

    @PostMapping
    @Operation(
            summary = "성취 자기신고 등록",
            description = """
                    사용자가 직접 등록하는 성취를 생성한다. source는 항상 SELF_REPORTED로 저장된다.

                    type에 따라 detail의 필수 필드가 다르다.
                    - CONTEST(수상·대회) : contestName 필수
                    - CHECKLIST(자격/도전/자유 목표) : title 필수
                    - PROJECT : 파티 확정 시 시스템이 자동 생성하는 전용 타입이라 400-4로 거부된다.
                      개인 사이드 프로젝트를 남기려면 CHECKLIST를 사용한다.

                    예외
                    - 400-1 : type/status/detail 누락 등 형식 검증 실패
                    - 400-4 : PROJECT 등록 시도, 타입별 필수 필드 누락
                    - 401-1 : 미로그인
                    """
    )
    public RsData<GoalDto> create(
            @Valid @RequestBody GoalCreateReqBody request
    ) {
        GoalDetailReqBody detail = request.detail();

        GoalDto goalDto = goalService.createSelfReported(
                rq.getActorFromDb(),
                new GoalService.SelfReportedSpec(
                        request.type(),
                        request.status(),
                        detail.contestName(),
                        detail.isTeam(),
                        detail.result(),
                        detail.awardDate(),
                        detail.title(),
                        detail.memo(),
                        detail.targetDate()
                )
        );

        return new RsData<>(
                "201-1",
                "성취 등록 성공",
                goalDto
        );
    }
}
