package com.back.global.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2RedirectUrlValidatorTest {
    private final OAuth2RedirectUrlValidator validator =
            new OAuth2RedirectUrlValidator("https://frontend.example");

    @Test
    void allowsInternalRelativePath() {
        assertThat(validator.getSafeRedirectUrl("/login?from=profile"))
                .isEqualTo("https://frontend.example/login?from=profile");
    }

    @Test
    void blocksRelativePathThatBecomesAbsoluteAfterRemovingLeadingSlash() {
        assertThat(validator.getSafeRedirectUrl("/http://evil.com"))
                .isEqualTo("https://frontend.example/");
        assertThat(validator.getSafeRedirectUrl("/https://evil.com"))
                .isEqualTo("https://frontend.example/");
    }
}
