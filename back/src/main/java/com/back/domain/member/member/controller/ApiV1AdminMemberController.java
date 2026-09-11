package com.back.domain.member.member.controller;

import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.member.member.dtos.MemberDetailDto;
import com.back.domain.member.member.dtos.MemberHistoryDto;
import com.back.domain.member.member.dtos.MemberListItemDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Role;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.party.party.service.PartyService;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/adm/members")
@RequiredArgsConstructor
public class ApiV1AdminMemberController {
    private final MemberService memberService;
    private final PartyService partyService;
    private final GoalService goalService;

    @GetMapping
    public RsData<Page<MemberListItemDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MemberListItemDto> members = memberService.getListForAdmin(
                keyword, role, active, PageRequest.of(page, size)
        );
        return new RsData<>("200-1", "관리자 회원 목록 조회 성공", members);
    }

    @GetMapping("/{member-id}")
    public RsData<MemberDetailDto> detail(
            @PathVariable("member-id") long memberId) {
        return new RsData<>("200-1", "관리자 회원 상세 조회 성공", memberService.getDetailForAdmin(memberId));
    }

    public record UpdateStatusReqBody(@NotNull Boolean active, String reason) {}

    @PatchMapping("/{member-id}/status")
    public RsData<Void> updateStatus(
            @PathVariable("member-id") long memberId, @Valid @RequestBody UpdateStatusReqBody request) {
        memberService.updateStatus(memberId, request.active(), request.reason());
        return new RsData<>(
                "200-1",
                request.active() ? "회원 정지 해제 성공" : "회원 정지 성공",
                null
        );
    }

    @GetMapping("/{member-id}/history")
    public RsData<MemberHistoryDto> history(
            @PathVariable("member-id") long memberId) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        MemberHistoryDto history = new MemberHistoryDto(
                partyService.getHistoryForAdmin(member),
                goalService.getAchievementsForAdmin(member)
        );

        return new RsData<>("200-1", "회원 이력 조회 성공", history);
    }
}
