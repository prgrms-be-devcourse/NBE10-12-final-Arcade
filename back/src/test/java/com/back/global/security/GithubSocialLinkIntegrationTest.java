package com.back.global.security;

import com.back.RedisTestContainerConfig;
import com.back.domain.member.auth.service.AuthService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(properties = "custom.frontend.base-url=http://localhost:3000")
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestContainerConfig.class)
class GithubSocialLinkIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("일반 로그인 계정은 GitHub 계정을 연결할 수 있다")
    void linkGithubSocialToCurrentMember() {
        Member actor = saveMember("member@test.com");

        authService.linkGithubSocial(actor, "GITHUB__12345", "github@test.com", "https://example.com/avatar.png");

        Member linkedMember = memberRepository.findById(actor.getId()).orElseThrow();
        assertThat(linkedMember.getGithubProviderUserId())
                .isEqualTo("GITHUB__12345");
        assertThat(linkedMember.getGithubEmail())
                .isEqualTo("github@test.com");
        assertThat(linkedMember.getProfileImgUrl())
                .isEqualTo("https://example.com/avatar.png");
    }

    @Test
    @DisplayName("다른 회원에 연결된 GitHub 계정은 연결할 수 없다")
    void cannotLinkGithubAlreadyConnectedToAnotherMember() {
        Member linkedMember = saveMember("linked@test.com");
        linkedMember.setGithubSocial("GITHUB__12345", "github@test.com");
        Member actor = saveMember("actor@test.com");

        assertThatThrownBy(() ->
                authService.linkGithubSocial(
                        actor,
                        "GITHUB__12345",
                        "github@test.com",
                        "https://example.com/avatar.png"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("409-1");
    }

    @Test
    @DisplayName("GitHub가 이미 연결된 계정의 GitHub OAuth 시작 요청은 오류 state와 함께 프론트로 리다이렉트된다")
    void blocksDuplicateGithubOAuthAuthorizationRequest() throws Exception {
        Member actor = saveMember("github-linked@test.com");
        actor.setGithubSocial("GITHUB__12345", "github@test.com");

        mvc.perform(get("/oauth2/authorization/github")
                        .param("redirectUrl", "/login?from=profile")
                        .cookie(new Cookie("apiKey", actor.getApiKey())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost:3000/login?from=profile&state=400-1"));
    }

    @Test
    @DisplayName("소셜 로그인 차단 시 설정된 프론트 origin의 절대 redirectUrl은 허용된다")
    void allowsConfiguredFrontendRedirectUrl() throws Exception {
        Member actor = saveMember("github-linked-frontend@test.com");
        actor.setGithubSocial("GITHUB__12345", "github@test.com");

        mvc.perform(get("/oauth2/authorization/github")
                        .param("redirectUrl", "http://localhost:3000/login?from=profile")
                        .cookie(new Cookie("apiKey", actor.getApiKey())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost:3000/login?from=profile&state=400-1"));
    }

    @Test
    @DisplayName("소셜 로그인 차단 시 외부 redirectUrl은 홈으로 대체된다")
    void blocksExternalRedirectUrl() throws Exception {
        Member actor = saveMember("github-linked-external@test.com");
        actor.setGithubSocial("GITHUB__12345", "github@test.com");

        mvc.perform(get("/oauth2/authorization/github")
                .param("redirectUrl", "https://malicious.example")
                        .cookie(new Cookie("apiKey", actor.getApiKey())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "http://localhost:3000/?state=400-1"));
    }

    private Member saveMember(String email) {
        return memberRepository.save(new Member(email, "password", "테스트 사용자", null));
    }
}
