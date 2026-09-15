package com.back.domain.member.member.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberGithubUserIdTest {
    @Test
    void linksGitHubAppUserWithoutSocialLoginAndVerifiesLaterSocialLink() {
        Member member = new Member("member@test.com", "password", "회원", null);
        member.linkGithubAppUserId(12345L);

        assertThat(member.getGithubUserId()).isEqualTo(12345L);
        member.setGithubSocial("GITHUB__12345", "github@test.com");
        assertThat(member.getGithubProviderUserId()).isEqualTo("GITHUB__12345");
        assertThatThrownBy(() -> member.setGithubSocial("GITHUB__99999", "other@test.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
