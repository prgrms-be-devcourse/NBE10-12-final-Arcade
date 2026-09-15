package com.back.domain.member.member.controller;

import com.back.domain.member.member.dtos.MemberDto;
import com.back.domain.member.member.dtos.MemberLoginDto;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
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
        String name,
        /**
         * 이용약관 동의. 필수다.
         *
         * @AssertTrue 를 쓰면 false 일 때만 걸리고 필드를 아예 빼면 통과하므로,
         * 누락과 거부를 함께 막으려고 @NotNull 로 받고 값 검증은 아래에서 한다.
         */
        @NotNull
        Boolean agreedTerms,
        /** 개인정보 수집·이용 동의. 필수다 */
        @NotNull
        Boolean agreedPrivacy
    ) {
        /** 필수 약관 둘 다 동의했는지 */
        public boolean agreedToRequiredTerms() {
            return Boolean.TRUE.equals(agreedTerms) && Boolean.TRUE.equals(agreedPrivacy);
        }
    }

    @PostMapping("/signup")
    @Operation(
            summary = "회원가입",
            description = """
                    이메일, 비밀번호, 이름으로 새 회원을 등록한다.
                    이메일은 이메일 형식이어야 하며, 이미 등록된 이메일은 사용할 수 없다.

                    agreedTerms·agreedPrivacy 는 **필수** 약관 동의다. 둘 다 true 여야 가입된다.
                    GitHub 가입자는 폼이 없어 이 경로를 타지 않고, 가입 뒤 POST /members/me/agreements 로 동의한다.

                    예외
                    - 400-1 : 이메일·비밀번호·이름·동의 누락 또는 이메일 형식 검증 실패
                    - 400-3 : 필수 약관에 동의하지 않음
                    - 409-1 : 이미 사용 중인 이메일
                    """
    )
    public RsData<MemberDto> signup(
        @Valid @RequestBody MemberSignupReqBody request
    ) {
        // 값이 false 인 경우는 Bean Validation 으로 못 잡는다(@NotNull 은 누락만 본다)
        if (!request.agreedToRequiredTerms()) {
            throw new ServiceException("400-3", "필수 약관에 모두 동의해야 가입할 수 있습니다.");
        }

        return new RsData<>(
            "201-1",
            "회원 생성 성공",
            memberService.join(request.email, request.password, request.name,
                    request.agreedToRequiredTerms())
        );
    }

    @PostMapping("/me/agreements")
    @Operation(
            summary = "필수 약관 동의",
            description = """
                    이용약관과 개인정보 수집·이용에 동의한 시점을 기록한다.

                    GitHub 로그인은 가입 폼이 없어 가입 과정에서 동의를 받을 자리가 없다.
                    그래서 콜백 뒤 온보딩 화면에서 이 API 로 동의를 남긴다.

                    프로필 수정(PATCH /members/me)과 나눠 둔 것은 그쪽이 언제든 불리는 API 라,
                    섞으면 **닉네임을 고칠 때마다 동의 일시가 갱신**되기 때문이다.

                    **이미 동의한 계정은 시각을 덮어쓰지 않는다** - 최초 동의 시점이 증빙이라
                    재요청·재시도로 날짜가 밀리면 안 된다. 그래서 여러 번 불러도 안전하다.

                    동의 일시는 GET /members/me 응답의 termsAgreedAt·privacyAgreedAt 으로 확인한다.

                    예외
                    - 401-1 : 미로그인
                    """
    )
    public RsData<Void> agreeToRequiredTerms() {
        memberService.agreeToRequiredTerms(rq.getActorFromDb());

        return new RsData<>("200-1", "약관 동의 완료");
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
