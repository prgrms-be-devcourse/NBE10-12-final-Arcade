package com.back.domain.member.member.dtos;

import com.back.domain.member.member.dtos.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetReqBody(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String token,
        @NotBlank @ValidPassword String newPassword,
        @NotBlank String newPasswordConfirm
) {
}
