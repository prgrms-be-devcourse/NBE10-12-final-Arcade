package com.back.domain.goal.goal.controller;

import com.back.domain.goal.goal.dtos.GoalCreateReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailResponseDto;
import com.back.domain.goal.goal.dtos.GoalDto;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.service.GoalService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/me")
    @Operation(
            summary = "내 성취 목록 조회",
            description = """
                    로그인한 회원 본인의 성취만 돌려준다.

                    status·type·source 는 모두 선택이며, 넘기지 않으면 해당 조건 없이 조회한다.
                    최신순(생성일 내림차순)으로 정렬한다.

                    예외
                    - 400-1 : 필터 값이 정의된 enum 이 아님
                    - 401-1 : 미로그인
                    """
    )
    public RsData<Page<GoalDto>> getMyGoals(
            @Parameter(description = "진행 상태 필터")
            @RequestParam(required = false) GoalStatus status,

            @Parameter(description = "성취 유형 필터")
            @RequestParam(required = false) GoalType type,

            @Parameter(description = "출처 필터. 자기신고 / 플랫폼 자동기록")
            @RequestParam(required = false) GoalSource source,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<GoalDto> goals = goalService.getMyGoals(
                rq.getActorFromDb(),
                status,
                type,
                source,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createDate"))
        );

        return new RsData<>(
                "200-1",
                "내 성취 목록 조회 성공",
                goals
        );
    }

    @GetMapping("/{goalId}")
    @Operation(
            summary = "성취 상세 조회",
            description = """
                    성취는 별도의 공개 범위 설정 없이 전체 공개라(기획서 2.5, 3.7) 남의 성취도 조회할 수 있다.

                    type 이 PROJECT 면 가리키는 파티 정보(project)가 함께 온다 - 파티명, 진행 상태, 모집 기한,
                    등록된 GitHub 저장소 주소, 그 파티에서 맡은 포지션, 파티장 여부.
                    PROJECT 성취는 내용을 직접 갖지 않고 파티를 가리키기만 하므로, 상세 화면은 이 정보로 구성한다.

                    동기화된 PR 목록(project.pullRequests)은 본인 성취를 볼 때만 채운다.
                    파티의 진행 기록은 파티원에게만 열려 있는 정보라(기획서 9.3) 남의 성취에서는 빈 배열이다.

                    기획서 9.4 는 비인증 조회로 정의하지만, 서버 인가 규칙상 아직은 로그인이 필요하다.

                    예외
                    - 401-1 : 미로그인
                    - 404-1 : 존재하지 않는 성취
                    """
    )
    public RsData<GoalDetailResponseDto> getGoal(
            @PathVariable long goalId
    ) {
        GoalDetailResponseDto goalDto = goalService.getGoal(rq.getActorFromDb(), goalId);

        return new RsData<>(
                "200-1",
                "성취 상세 조회 성공",
                goalDto
        );
    }
}
