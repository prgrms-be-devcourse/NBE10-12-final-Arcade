package com.back.domain.member.member.controller;

import com.back.domain.member.member.dtos.MemberDto;
import com.back.domain.member.member.dtos.MemberLoginDto;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ApiV1MemberController {

    private final MemberService memberService;
    private final Rq rq;

    public record MemberSignupReqBody(
        @NotNull
        @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"
        )
        String email,
        @NotNull
        String password,
        @NotNull
        String name
    ) { }

    @PostMapping("/signup")
    public RsData<MemberDto> signup(
        @Valid @RequestBody MemberSignupReqBody request
    ) {
        return new RsData<>(
            "201-1",
            "회원 생성 성공",
            memberService.join(request.email, request.password, request.name)
        );
    }

    public record MemberLoginReqBody(
        @NotNull
        @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"
            )
        String email,
        @NotNull
        String password
    ) { }

    @PostMapping("/login")
    public RsData<MemberLoginDto> login(
        @Valid @RequestBody MemberLoginReqBody request
    ) {
        MemberLoginDto loginDto = memberService.login(request.email, request.password);

        rq.setCookie("accessToken", loginDto.accessToken());
        rq.setCookie("refreshToken", loginDto.refreshToken());

        return new RsData<>(
            "201-1",
            "로그인 성공",
            loginDto
        );
    }

    @PostMapping("/logout")
    public RsData<Void> logout() {
        rq.deleteCookie("refreshToken");
        rq.deleteCookie("accessToken");
        return new RsData<>(
            "200-1",
            "로그아웃 되었습니다."
        );
    }

    public record RefreshTokenReqBody(
        @NotNull
        String refreshToken
    ) { }

    @PostMapping("/refresh")
    public RsData<MemberLoginDto> refresh(
        @Valid @RequestBody RefreshTokenReqBody request
    ) {
        MemberLoginDto loginDto = memberService.refreshToken(request.refreshToken);

        rq.setCookie("accessToken", loginDto.accessToken());
        rq.setCookie("refreshToken", loginDto.refreshToken());

        return new RsData<>(
            "201-1",
            "AccessToken 재발급 성공",
            loginDto
        );
    }

}
