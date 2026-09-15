package com.back.domain.member.member.dtos;

import com.back.domain.member.member.dtos.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeReqBody(
        @NotBlank String currentPassword,
        @NotBlank @ValidPassword String newPassword,
        @NotBlank String newPasswordConfirm
) {
}
