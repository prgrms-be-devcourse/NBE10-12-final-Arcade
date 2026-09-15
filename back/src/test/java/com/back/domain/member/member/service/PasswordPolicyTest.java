package com.back.domain.member.member.service;

import com.back.global.app.CustomConfigProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {
    private final CustomConfigProperties properties = new CustomConfigProperties();
    private final PasswordPolicy passwordPolicy = new PasswordPolicy(properties);

    @Test
    void acceptsPasswordMatchingDefaultPolicy() {
        assertThat(passwordPolicy.isValid("Password!1")).isTrue();
    }

    @Test
    void rejectsPasswordMissingRequiredCharacterTypeOrContainingWhitespace() {
        assertThat(passwordPolicy.isValid("1234567!8")).isFalse();
        assertThat(passwordPolicy.isValid("Password12")).isFalse();
        assertThat(passwordPolicy.isValid("Password !1")).isFalse();
    }

    @Test
    void appliesConfiguredPolicyInsteadOfHardCodedRule() {
        properties.getPassword().setMinLength(4);
        properties.getPassword().setRequireSpecialCharacter(false);

        assertThat(passwordPolicy.isValid("Abc1")).isTrue();
    }
}
