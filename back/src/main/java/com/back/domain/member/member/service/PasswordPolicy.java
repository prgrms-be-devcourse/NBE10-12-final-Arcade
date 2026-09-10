package com.back.domain.member.member.service;

import com.back.global.app.CustomConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 규칙을 한 곳에서 관리한다. DTO 검증과 비밀번호 변경 서비스는 이 컴포넌트를 사용한다.
 * 역할별 정책이 필요해질 때에는 이 클래스에 역할별 정책 선택만 추가하면 된다.
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {
    private final CustomConfigProperties customConfigProperties;

    public boolean isValid(String password) {
        if (password == null) return false;

        CustomConfigProperties.Password policy = customConfigProperties.getPassword();
        if (!policy.isLengthRangeValid()
                || password.length() < policy.getMinLength()
                || password.length() > policy.getMaxLength()) {
            return false;
        }
        if (!policy.isAllowWhitespace() && password.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        if (policy.isRequireLetter() && password.chars().noneMatch(Character::isLetter)) {
            return false;
        }
        if (policy.isRequireNumber() && password.chars().noneMatch(Character::isDigit)) {
            return false;
        }

        return !policy.isRequireSpecialCharacter()
                || password.chars().anyMatch(character -> !Character.isLetterOrDigit(character)
                        && !Character.isWhitespace(character));
    }
}
