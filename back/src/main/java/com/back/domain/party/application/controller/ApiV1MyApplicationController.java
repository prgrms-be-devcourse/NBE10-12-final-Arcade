package com.back.domain.party.application.controller;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.dtos.MyApplicationDto;
import com.back.domain.party.application.dtos.ReceivedApplicationDto;
import com.back.domain.party.application.service.PartyApplicationService;
import com.back.domain.party.party.dtos.MyPartyDto;
import com.back.domain.party.party.service.MyPartyService;
import com.back.global.dto.SliceDto;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 지원자·파티장 본인 기준의 지원 조회. 파티를 먼저 고르지 않고 내 것을 한 번에 본다.
 * 파티 단위 조회·승인은 ApiV1PartyApplicationController 쪽이다.
 */
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
@Validated
@Tag(name = "ApiV1MyApplicationController", description = "내 지원 현황 컨트롤러")
public class ApiV1MyApplicationController {

    private final PartyApplicationService partyApplicationService;
    private final MyPartyService myPartyService;
    private final Rq rq;

    @GetMapping("/parties")
    @Operation(
            summary = "참여 파티 히스토리 조회",
            description = """
                    **파티 확정 명단에 내가 있는 파티**를 최근 개설순으로 돌려준다(기획서 2.11).

                    - topicType   : 주제 유형(CONTEST/PROJECT/STUDY/ETC). 화면이 아이콘으로 그린다
                    - role        : OWNER(파티장) / MEMBER(파티원)
                    - status      : IN_PROGRESS / COMPLETED (확정된 파티만 담긴다)
                    - positionType: 내가 맡은 자리, 파티장은 마이페이지에 선택된 값
                    - exhibited   : 전시가 게시됐는지. true 일 때만 전시 페이지로 연결한다

                    페이징하지 않는다. 한 사람이 속한 파티는 많아야 수십 건이고,
                    화면이 시간순 타임라인이라 끊으면 연도 묶음이 잘린다(성취 목록과 같은 판단).

                    예외
                    - 401-1 : 미로그인
                    """
    )
    public RsData<List<MyPartyDto>> getMyParties() {
        return new RsData<>(
                "200-1",
                "참여 파티 히스토리 조회 성공",
                myPartyService.getMyParties(rq.getActorFromDb())
        );
    }

    @GetMapping("/applications")
    @Operation(
            summary = "내 지원 현황 조회",
            description = """
                    로그인한 회원이 지원한 내역 중 승인대기(PENDING) 건만 최신순으로 조회한다.
                    파티로 이동해야 하므로 party.id 를 함께 싣는다.
                    예외
                    - 400-1 : page·size 범위 검증 실패
                    - 401-1 : 미로그인
                    """
    )
    public RsData<SliceDto<MyApplicationDto>> getMyApplications(
            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 1~100, 기본값 20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new RsData<>(
                "200-1",
                "내 지원 현황 조회 성공",
                partyApplicationService.getMyApplications(rq.getActorFromDb(), page, size)
        );
    }

    @GetMapping("/received-applications")
    @Operation(
            summary = "받은 지원자 관리 조회",
            description = """
                    로그인한 회원이 파티장인 파티들에 들어온 지원을 최신순으로 조회한다.
                    파티를 먼저 고르지 않아도 되고, partyId 를 주면 그 파티만 본다.

                    상태로 거르지 않는다 - 파티 단위 조회(GET /parties/{partyId}/applications)와 같은 기준이고,
                    화면이 대기·승인·거절 탭으로 나눠 쓸 수 있게 그대로 내려준다.

                    전체 건수를 쓰지 않는 화면이라 count 쿼리 없이 hasNext 만 내려준다 -
                    응답에 totalElements·totalPages 는 없다.

                    카드 하나에서 승인/거절을 판단할 수 있게 지원자의 프로필과 성취를 함께 싣는다(기획서 2.1).
                    position 은 이번에 지원한 포지션이고, applicant.preferredPosition 은 프로필에 적어둔 희망 포지션이라 서로 다른 값이다.
                    프로필을 만들지 않은 회원이면 nickname·preferredPosition 이 null 이고 techStacks 는 빈 배열이다.
                    achievements 는 건수만 준다 - 출처(자동기록/자기신고)로 나눈 두 값이고, 목록이 필요하면 성취 API 를 쓴다.

                    예외
                    - 400-1 : page·size 범위 검증 실패, part 가 정의된 포지션이 아님
                    - 401-1 : 미로그인
                    """
    )
    public RsData<SliceDto<ReceivedApplicationDto>> getReceivedApplications(
            @Parameter(description = "특정 파티만 볼 때 지정한다. 생략하면 내 파티 전체")
            @RequestParam(required = false) Long partyId,
            @Parameter(description = "포지션 필터: BACK/FRONT. 생략하면 전체")
            @RequestParam(required = false) PositionType part,
            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 1~100, 기본값 20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new RsData<>(
                "200-1",
                "받은 지원자 목록 조회 성공",
                partyApplicationService.getReceivedApplications(
                        rq.getActorFromDb(), partyId, part, page, size)
        );
    }
}
