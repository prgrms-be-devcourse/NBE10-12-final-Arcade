package com.back.domain.member.member.controller;

import com.back.domain.member.member.dtos.MemberDto;
import com.back.domain.member.member.dtos.MemberLoginDto;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "ApiV1MemberController", description = "회원 컨트롤러")
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
    @Operation(
            summary = "회원가입",
            description = """
                    이메일, 비밀번호, 이름으로 새 회원을 등록한다.
                    이메일은 이메일 형식이어야 하며, 이미 등록된 이메일은 사용할 수 없다.

                    예외
                    - 400-1 : 이메일·비밀번호·이름 누락 또는 이메일 형식 검증 실패
                    - 409-1 : 이미 사용 중인 이메일
                    """
    )
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
    @Operation(
            summary = "로그인",
            description = """
                    이메일과 비밀번호를 검증해 로그인한다.
                    성공하면 accessToken과 refreshToken을 응답 및 쿠키에 설정한다.

                    예외
                    - 400-1 : 이메일·비밀번호 누락 또는 이메일 형식 검증 실패
                    - 401-2 : 이메일 또는 비밀번호가 올바르지 않음
                    """
    )

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
    @Operation(
            summary = "로그아웃",
            description = """
                    브라우저에 저장된 accessToken과 refreshToken 쿠키를 삭제한다.
                    """
    )
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
    @Operation(
            summary = "토큰 재발급",
            description = """
                    refreshToken으로 새로운 accessToken과 refreshToken을 발급한다.
                    refreshToken은 한 번 사용하면 재사용할 수 없으며, 새 토큰은 응답 및 쿠키에 설정된다.

                    예외
                    - 400-1 : refreshToken 누락
                    - 401-3 : 유효하지 않거나 만료된 refreshToken
                    - 401-4 : 이미 사용한 refreshToken
                    - 404-1 : 토큰에 연결된 회원이 없음
                    """
    )
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
