package com.back.domain.member.member.dtos.validation;

import com.back.domain.member.member.service.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {
    private final PasswordPolicy passwordPolicy;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // null과 빈 값은 @NotBlank가 별도로 처리한다.
        return password == null || password.isBlank() || passwordPolicy.isValid(password);
    }
}
