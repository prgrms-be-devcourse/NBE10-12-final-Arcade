package com.back.domain.member.profile.controller;

import com.back.domain.member.profile.dtos.MemberProfileDto;
import com.back.domain.member.profile.service.MemberProfileService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ApiV1MemberProfileController {

    private final MemberProfileService memberProfileService;
    private final Rq rq;

    @GetMapping("/me")
    public RsData<MemberProfileDto> me() {
        return new RsData<>(
                "200-1",
                "내 정보 조회 성공",
                memberProfileService.me(rq.getActorFromDb())
        );
    }

    public record ModifyProfileReqBody(
            @NotNull String nickname,
            String webpage,
            String profileImageUrl,
            @NotNull List<@NotBlank String> positions,
            @NotNull List<@NotBlank String> techStacks
    ) {
    }

    @PatchMapping("/me")
    public RsData<MemberProfileDto> modifyProfile(
            @Valid @RequestBody ModifyProfileReqBody request
    ) {
        return new RsData<>(
                "200-1",
                "내 정보 수정 성공",
                memberProfileService.modifyProfile(
                        rq.getActorFromDb(),
                        request.nickname,
                        request.webpage,
                        request.profileImageUrl,
                        request.positions,
                        request.techStacks
                )
        );
    }

}
