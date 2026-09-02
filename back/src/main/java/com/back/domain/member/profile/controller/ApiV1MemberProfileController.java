package com.back.domain.member.profile.controller;

import com.back.domain.member.profile.dtos.MemberProfileDto;
import com.back.domain.member.profile.service.MemberProfileService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "ApiV1MemberProfileController", description = "회원 프로필 컨트롤러")
public class ApiV1MemberProfileController {

    private final MemberProfileService memberProfileService;
    private final Rq rq;

    @GetMapping("/me")
    @Operation(
            summary = "내 프로필 조회",
            description = """
                    로그인한 회원의 프로필을 조회한다.
                    아직 프로필이 없으면 기본 프로필을 생성한 뒤 반환한다.

                    예외
                    - 401-1 : 미로그인
                    """
    )
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
    @Operation(
            summary = "내 프로필 수정",
            description = """
                    로그인한 회원의 닉네임, 웹페이지, 프로필 이미지, 희망 포지션과 기술 스택을 수정한다.
                    positions와 techStacks는 빈 값이 아닌 문자열 목록으로 전달해야 한다.

                    예외
                    - 400-1 : nickname·positions·techStacks 누락 또는 목록 원소가 빈 문자열
                    - 401-1 : 미로그인
                    - 409-1 : 이미 사용 중인 닉네임
                    """
    )
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
