package com.back.domain.member.profile.controller;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
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
            String bio,
            String githubUsername,
            PositionType position,
            List<@NotBlank String> techStacks,
            List<CareerCommand> careers,
            List<LinkCommand> links
    ) {
    }


    @PatchMapping("/me")
    @Operation(
            summary = "내 프로필 수정",
            description = """
                    로그인한 회원의 닉네임, 웹페이지, 프로필 이미지, 소개,
                    희망 포지션, 기술 스택, 경력, 링크를 수정한다.
                    nickname 외에는 모두 선택이고, 보낸 값이 곧 저장될 값이다 -
                    생략하면 그 항목이 비워진다. 화면이 폼 전체를 보내오는 것을 전제로 한 규칙이다.

                    position은 대표 포지션 하나이고 BACK/FRONT/UIUX/PM 중 하나다.
                    techStacks의 원소는 빈 문자열이면 400-1이다.

                    careers·links는 보낸 목록이 곧 저장될 목록이다 - 생략하거나 빈 배열을 보내면 전부 지운다.
                    role(경력) 또는 label·url(링크)이 비어 있는 항목은 무시한다.

                    profileImageUrl은 직접 올린 이미지만 담는다. 조회 응답은 이 값과 githubAvatarUrl을
                    합치지 않고 그대로 내려주니, 화면이 profileImageUrl ?? githubAvatarUrl로 고르면 된다.
                    githubAvatarUrl을 수정 요청에 실으면 아바타 주소가 '직접 올린 것'으로 굳어
                    GitHub에서 바꿔도 반영되지 않으니 넣지 말 것.

                    예외
                    - 400-1 : nickname 누락 또는 techStacks 원소가 빈 문자열
                    - 400-2 : position이 정의된 값이 아님
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
                        request.bio,
                        request.githubUsername,
                        request.position,
                        request.techStacks,
                        request.careers,
                        request.links
                )
        );
    }



}
