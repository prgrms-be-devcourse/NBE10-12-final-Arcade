package com.back.domain.goal.goal.controller;

import com.back.domain.goal.goal.dtos.GoalCreateReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailResponseDto;
import com.back.domain.goal.goal.dtos.GoalDto;
import com.back.domain.goal.goal.dtos.GoalUpdateReqBody;
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

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
        GoalDto goalDto = goalService.createSelfReported(rq.getActorFromDb(), request);

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

                    status·type·source·year·keyword 는 모두 선택이며, 넘기지 않으면 해당 조건 없이 조회한다.
                    마이페이지 연혁 화면의 필터(상태·유형·출처·연도)와 같은 축이다.
                    최신순(생성일 내림차순)으로 정렬한다.

                    페이징하지 않고 조건에 맞는 성취를 전부 돌려준다.
                    연혁은 한 사람의 이력 전체를 연도별로 묶어 보여주는 화면이라, 끊어서 주면 연도 그룹과
                    연도 선택지가 잘린다. 회원당 성취는 많아야 수백 건 규모다.

                    year 가 보는 '성취의 대표 날짜'는 타입마다 다르다.
                    PROJECT 는 참여 시작일, CONTEST 는 수상일, CHECKLIST 는 목표일이고,
                    그 값이 없으면 등록일로 떨어진다.

                    keyword 는 제목·대회명·수상 결과·메모를 부분 일치로 훑는다. 대소문자는 구분하지 않는다.

                    예외
                    - 400-1 : 필터 값이 정의된 enum 이 아니거나 year 가 숫자가 아님
                    - 401-1 : 미로그인
                    """
    )
    public RsData<List<GoalDto>> getMyGoals(
            @Parameter(description = "진행 상태 필터")
            @RequestParam(required = false) GoalStatus status,

            @Parameter(description = "성취 유형 필터")
            @RequestParam(required = false) GoalType type,

            @Parameter(description = "출처 필터. 자기신고 / 플랫폼 자동기록")
            @RequestParam(required = false) GoalSource source,

            @Parameter(description = "연도 필터. 성취의 대표 날짜가 이 연도인 것만", example = "2026")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "검색어. 제목·대회명·수상 결과·메모에 대한 부분 일치(대소문자 무시)")
            @RequestParam(required = false) String keyword
    ) {
        List<GoalDto> goals = goalService.getMyGoals(
                rq.getActorFromDb(),
                status,
                type,
                source,
                year,
                keyword
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

    @PatchMapping("/{goalId}")
    @Operation(
            summary = "성취 수정",
            description = """
                    본인이 직접 등록한(SELF_REPORTED) 성취만 수정할 수 있다.
                    파티 활동으로 자동기록된 성취는 크루온이 보증하는 기록이라 직접 고칠 수 없다(기획서 9.4).

                    status 와 detail 은 각각 생략할 수 있다.
                    - status 만 보내면 상태만 바꾼다
                    - detail 만 보내면 내용만 바꾼다
                    - status 가 현재 상태와 같으면 전이로 보지 않고 통과시킨다 (내용만 고치는 경우)

                    detail 을 보냈다면 그 타입의 세부 정보는 통째로 교체된다.
                    화면이 폼 전체를 보내오므로, 넘기지 않은 항목은 유지가 아니라 비우기로 처리된다.

                    type 은 바꿀 수 없다 - 다른 유형으로 남기려면 새로 등록해야 한다.

                    예외
                    - 400-1 : 형식 검증 실패
                    - 400-4 : 타입별 필수 필드 누락 (CONTEST-contestName, CHECKLIST-title)
                    - 401-1 : 미로그인
                    - 403-1 : 남의 성취
                    - 404-1 : 존재하지 않는 성취
                    - 409-1 : 자동기록된 성취 수정 시도
                    - 409-2 : 허용되지 않는 상태 변경 (ACHIEVED 는 종료 상태라 되돌릴 수 없다)
                    """
    )
    public RsData<GoalDto> update(
            @PathVariable long goalId,
            @Valid @RequestBody GoalUpdateReqBody request
    ) {
        GoalDto goalDto = goalService.updateSelfReported(rq.getActorFromDb(), goalId, request);

        return new RsData<>(
                "200-1",
                "성취 수정 성공",
                goalDto
        );
    }

    @DeleteMapping("/{goalId}")
    @Operation(
            summary = "성취 삭제",
            description = """
                    본인이 직접 등록한(SELF_REPORTED) 성취만 삭제할 수 있다.
                    파티 활동으로 자동기록된 성취는 크루온이 보증하는 기록이라 지울 수 없다(기획서 9.4).

                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 남의 성취
                    - 404-1 : 존재하지 않는 성취
                    - 409-1 : 자동기록된 성취 삭제 시도
                    """
    )
    public RsData<Void> delete(
            @PathVariable long goalId
    ) {
        goalService.deleteSelfReported(rq.getActorFromDb(), goalId);

        return new RsData<>(
                "204-1",
                "성취 삭제 성공",
                null
        );
    }
}
