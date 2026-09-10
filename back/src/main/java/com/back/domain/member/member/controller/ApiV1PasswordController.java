package com.back.domain.member.member.controller;

import com.back.domain.member.member.dtos.PasswordChangeReqBody;
import com.back.domain.member.member.dtos.PasswordResetReqBody;
import com.back.domain.member.member.dtos.PasswordResetRequestReqBody;
import com.back.domain.member.member.dtos.PasswordResetTokenValidityDto;
import com.back.domain.member.member.service.PasswordResetFacade;
import com.back.domain.member.member.service.PasswordService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "ApiV1PasswordController", description = "회원 비밀번호 재설정 및 변경")
public class ApiV1PasswordController {
    private static final String RESET_REQUEST_MESSAGE = "입력한 이메일로 가입된 계정이 있다면 비밀번호 재설정 링크를 전송했습니다.";

    private final PasswordResetFacade passwordResetFacade;
    private final PasswordService passwordService;
    private final Rq rq;

    @PostMapping("/password/reset-requests")
    @Operation(summary = "비밀번호 재설정 이메일 요청")
    public RsData<Void> requestReset(
            @Valid @RequestBody PasswordResetRequestReqBody request,
            HttpServletRequest servletRequest
    ) {
        passwordResetFacade.requestReset(request.email(), servletRequest.getRemoteAddr());
        return new RsData<>("200-1", RESET_REQUEST_MESSAGE);
    }

    @GetMapping("/password/reset-tokens/{token}")
    @Operation(summary = "비밀번호 재설정 토큰 유효성 확인")
    public RsData<PasswordResetTokenValidityDto> validateResetToken(@PathVariable String token) {
        passwordResetFacade.validateToken(token);
        return new RsData<>("200-1", "유효한 비밀번호 재설정 링크입니다.", new PasswordResetTokenValidityDto(true));
    }

    @PostMapping("/password/resets")
    @Operation(summary = "비밀번호 재설정")
    public RsData<Void> resetPassword(@Valid @RequestBody PasswordResetReqBody request) {
        passwordResetFacade.resetPassword(request.token(), request.newPassword(), request.newPasswordConfirm());
        return new RsData<>("200-1", "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
    }

    @PatchMapping("/me/password")
    @Operation(summary = "로그인 회원 비밀번호 변경")
    public RsData<Void> changePassword(@Valid @RequestBody PasswordChangeReqBody request) {
        passwordService.changePassword(rq.getActor(), request.currentPassword(), request.newPassword(), request.newPasswordConfirm());
        rq.deleteCookie("accessToken");
        rq.deleteCookie("refreshToken");
        return new RsData<>("200-1", "비밀번호가 변경되었습니다. 새 비밀번호로 다시 로그인해주세요.");
    }
}
