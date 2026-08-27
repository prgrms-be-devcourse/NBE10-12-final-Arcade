package com.back.domain.member.member.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithUserDetails;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1MemberControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("회원가입: 201-1과 회원 식별자를 반환한다")
    void signup() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "test@test.com",
                      "password": "1234",
                      "name": "정하늘"
                    }
                    """));

        resultActions.andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.msg").value("회원 생성 성공"))
            .andExpect(jsonPath("$.data.id").isNumber())
            .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }

    @Test
    @DisplayName("회원가입: 필드 검증 실패는 400-1이다")
    void signupWithInvalidField() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email":"not-an-email",
                        "password":"","name":""
                    }
                    """));

        resultActions.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("회원가입: JSON 본문 파싱 실패는 400-2이다")
    void signupWithMalformedJson() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"));

        resultActions.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @DisplayName("회원가입: 중복 이메일은 409-1이다")
    void signupWithDuplicatedEmail() throws Exception {
        ResultActions signupResultActions = mvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email":"duplicate@test.com",
                            "password":"1234",
                            "name":"정하늘"
                        }"""));

        signupResultActions.andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email":"duplicate@test.com",
                            "password":"1234",
                            "name":"정하늘"
                        }"""));

        resultActions.andExpect(status().isConflict())
            .andExpect(jsonPath("$.resultCode").value("409-1"))
            .andExpect(jsonPath("$.msg").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("로그인: 201-1과 Bearer 토큰 응답을 반환한다")
    void login() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email":"user1@test.com",
                        "password":"1234"
                    }"""));

        resultActions.andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.msg").value("로그인 성공"))
            .andExpect(jsonPath("$.data.grantType").value("Bearer"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(3600))
            .andExpect(jsonPath("$.data.role").value("MEMBER"));
    }

    @Test
    @DisplayName("로그인: 자격 증명 불일치는 401-2로 통일한다")
    void loginWithInvalidCredentials() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email":"user1@test.com",
                        "password":"wrong"
                    }"""));

        resultActions.andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-2"))
            .andExpect(jsonPath("$.msg").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("로그아웃")
    @WithUserDetails("user1@test.com")
    void logout() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/logout"))
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(ApiV1MemberController.class))
                .andExpect(handler().methodName("logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."));

        resultActions.andExpect(result -> {
            Cookie refreshTokenCookie = result.getResponse().getCookie("refreshToken");

            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.getValue()).isEmpty();
            assertThat(refreshTokenCookie.getMaxAge()).isZero();
            assertThat(refreshTokenCookie.getPath()).isEqualTo("/");
            assertThat(refreshTokenCookie.isHttpOnly()).isTrue();

            Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");

            assertThat(accessTokenCookie).isNotNull();
            assertThat(accessTokenCookie.getValue()).isEmpty();
            assertThat(accessTokenCookie.getMaxAge()).isZero();
            assertThat(accessTokenCookie.getPath()).isEqualTo("/");
            assertThat(accessTokenCookie.isHttpOnly()).isTrue();
        });
    }

    @Test
    @DisplayName("토큰 재발급: refreshToken body로 201-1과 access token을 반환한다")
    @WithUserDetails("user1@test.com")
    void refresh() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "refreshToken":"user1"
                    }"""));

        resultActions.andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.grantType").value("Bearer"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(3600));
    }

    @Test
    @DisplayName("토큰 재발급: 유효하지 않거나 만료된 refresh token은 401-3이다")
    @WithUserDetails("user1@test.com")
    void refreshWithInvalidToken() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "refreshToken":"expired-token"
                    }"""));

        resultActions.andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-3"));
    }


    /*
    @Test
    @DisplayName("토큰 재발급: 재사용된 refresh token은 401-4이고 전 기기 토큰을 폐기한다")
    @WithUserDetails("user1@test.com")
    void refreshWithReusedToken() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "refreshToken":"reused-token"
                    }"""));

        resultActions.andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-4"))
            .andExpect(jsonPath("$.msg").value("토큰 재사용이 감지되어 모든 기기에서 로그아웃 처리되었습니다."));
    }

     */

    @Test
    @DisplayName("토큰 재발급: refreshToken 누락은 400-1이다")
    @WithUserDetails("user1@test.com")
    void refreshWithoutToken() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        resultActions.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400-1"));
    }
}
