package com.back.domain.member.member.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.service.PasswordResetTokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithUserDetails;

import jakarta.servlet.http.Cookie;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestContainerConfig.class)
class ApiV1PasswordControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordResetTokenStore passwordResetTokenStore;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void resetTokenCanBeCheckedWithoutBeingConsumedAndThenUsedOnce() throws Exception {
        Member member = memberRepository.save(new Member(uniqueEmail(), passwordEncoder.encode("OldPassword!1"), "회원", null));
        String token = passwordResetTokenStore.issue(member.getId());

        mvc.perform(get("/api/v1/members/password/reset-tokens/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.valid").value(true));

        mvc.perform(post("/api/v1/members/password/resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetRequest(token, "NewPassword!1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        assertThat(passwordEncoder.matches("NewPassword!1", member.getPassword())).isTrue();
        mvc.perform(post("/api/v1/members/password/resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetRequest(token, "AnotherPassword!1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));
    }

    @Test
    void resetRequestHidesMembershipAndRateLimitsRequests() throws Exception {
        String email = uniqueEmail();
        for (int attempt = 0; attempt < 3; attempt++) {
            mvc.perform(post("/api/v1/members/password/reset-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\"}".formatted(email)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultCode").value("200-1"));
        }

        mvc.perform(post("/api/v1/members/password/reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.resultCode").value("429-1"));
    }

    @Test
    void passwordChangeRequiresAuthentication() throws Exception {
        mvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"OldPassword!1","newPassword":"NewPassword!1","newPasswordConfirm":"NewPassword!1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    void invalidPasswordDoesNotConsumeValidResetToken() throws Exception {
        Member member = memberRepository.save(new Member(uniqueEmail(), passwordEncoder.encode("OldPassword!1"), "회원", null));
        String token = passwordResetTokenStore.issue(member.getId());

        mvc.perform(post("/api/v1/members/password/resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"invalid","newPasswordConfirm":"invalid"}
                                """.formatted(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        mvc.perform(get("/api/v1/members/password/reset-tokens/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    void invalidResetTokenReturnsSameErrorCode() throws Exception {
        mvc.perform(get("/api/v1/members/password/reset-tokens/{token}", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));
    }

    @Test
    @WithUserDetails("user1@test.com")
    void passwordChangeDeletesAuthenticationCookies() throws Exception {
        mvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"1234","newPassword":"MemberPassword!1","newPasswordConfirm":"MemberPassword!1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(result -> {
                    Cookie accessToken = result.getResponse().getCookie("accessToken");
                    Cookie refreshToken = result.getResponse().getCookie("refreshToken");
                    assertThat(accessToken).isNotNull();
                    assertThat(accessToken.getMaxAge()).isZero();
                    assertThat(refreshToken).isNotNull();
                    assertThat(refreshToken.getMaxAge()).isZero();
                });
    }

    private String uniqueEmail() {
        return "password-%s@test.com".formatted(UUID.randomUUID());
    }

    private String resetRequest(String token, String newPassword) {
        return """
                {"token":"%s","newPassword":"%s","newPasswordConfirm":"%s"}
                """.formatted(token, newPassword, newPassword);
    }
}
