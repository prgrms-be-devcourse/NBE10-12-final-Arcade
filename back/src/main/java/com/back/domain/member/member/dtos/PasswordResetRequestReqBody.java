package com.back.domain.member.member.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestReqBody(
        @NotBlank @Email String email
) {
}
