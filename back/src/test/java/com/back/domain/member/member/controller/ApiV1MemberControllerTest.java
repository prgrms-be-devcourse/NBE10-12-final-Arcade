package com.back.domain.member.member.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithUserDetails;

import jakarta.servlet.http.Cookie;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestContainerConfig.class)
public class ApiV1MemberControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("약관 동의: 가입 요청에 동의를 실으면 동의 일시가 기록된다")
    // 테스트 트랜잭션 밖에서 돌린다. 안에서 돌리면 컨트롤러가 그 트랜잭션에 얹혀
    // 쓰기 가능 상태가 되어, 서비스의 readOnly 문제(동의 일시가 flush 되지 않음)를 가린다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void signupWithAgreements() throws Exception {
        mvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "agreed@test.com",
                              "password": "1234",
                              "name": "동의한 사람",
                              "agreedTerms": true,
                              "agreedPrivacy": true
                            }
                            """))
                .andExpect(status().isCreated());

        Member member = memberRepository.findByEmail("agreed@test.com").orElseThrow();
        try {
            assertThat(member.getTermsAgreedAt()).isNotNull();
            assertThat(member.getPrivacyAgreedAt()).isNotNull();
            assertThat(member.hasAgreedToRequiredTerms()).isTrue();
        } finally {
            // 이 테스트만 트랜잭션 밖이라 롤백되지 않는다. 다음 실행에 영향이 없게 직접 지운다
            memberRepository.delete(member);
        }
    }

    @Test
    @DisplayName("약관 동의: 둘 중 하나만 동의하면 400-3 이다")
    void signupWithPartialAgreement() throws Exception {
        // 필수 둘 다여야 한다. 하나만 켠 요청을 통과시키면 증빙이 틀린 값이 된다
        mvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "partial@test.com",
                              "password": "1234",
                              "name": "반만 동의",
                              "agreedTerms": true,
                              "agreedPrivacy": false
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));

        assertThat(memberRepository.findByEmail("partial@test.com")).isEmpty();
    }

    @Test
    @DisplayName("약관 동의: 동의를 안 보내면 400 이라 가입되지 않는다")
    void signupWithoutAgreements() throws Exception {
        mvc.perform(post("/api/v1/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "notagreed@test.com",
                              "password": "1234",
                              "name": "동의 안 한 사람"
                            }
                            """))
                .andExpect(status().isBadRequest());

        assertThat(memberRepository.findByEmail("notagreed@test.com")).isEmpty();
    }

    @Test
    @DisplayName("약관 동의: 미로그인이면 401")
    void agreeWithoutLogin() throws Exception {
        mvc.perform(post("/api/v1/members/me/agreements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("약관 동의: 동의하면 두 일시가 채워지고, 다시 불러도 시각이 밀리지 않는다")
    @WithUserDetails("user1@test.com")
    void agreeToRequiredTerms() throws Exception {
        mvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.termsAgreedAt").doesNotExist())
                .andExpect(jsonPath("$.data.privacyAgreedAt").doesNotExist());

        mvc.perform(post("/api/v1/members/me/agreements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        String first = mvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.termsAgreedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.privacyAgreedAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // 최초 동의 시점이 증빙이라 재요청으로 덮어써지면 안 된다
        mvc.perform(post("/api/v1/members/me/agreements"))
                .andExpect(status().isOk());

        String second = mvc.perform(get("/api/v1/members/me"))
                .andReturn().getResponse().getContentAsString();

        assertThat(agreedAt(second)).isEqualTo(agreedAt(first));
    }

    private String agreedAt(String responseBody) {
        Matcher matcher = Pattern.compile("\"termsAgreedAt\":\"([^\"]+)\"").matcher(responseBody);

        return matcher.find() ? matcher.group(1) : null;
    }

    @Test
    @DisplayName("회원가입: 201-1과 회원 식별자를 반환한다")
    void signup() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "test@test.com",
                      "password": "1234",
                      "name": "정하늘",
                      "agreedTerms": true,
                      "agreedPrivacy": true
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
                            "name":"정하늘",
                            "agreedTerms":true,
                            "agreedPrivacy":true
                        }"""));

        signupResultActions.andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email":"duplicate@test.com",
                            "password":"1234",
                            "name":"정하늘",
                            "agreedTerms":true,
                            "agreedPrivacy":true
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
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(900))
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
    void refresh() throws Exception {
        ResultActions loginResultActions = mvc.perform(post("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email":"user1@test.com",
                        "password":"1234"
                    }"""))
            .andExpect(status().isCreated());

        String refreshToken = extractRefreshToken(loginResultActions);

        ResultActions resultActions = mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));

        resultActions.andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.data.grantType").value("Bearer"))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(900))
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("토큰 재발급: 유효하지 않거나 만료된 refresh token은 401-3이다")
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

    @Test
    @DisplayName("토큰 재발급: 재사용된 refresh token은 401-4이다")
    void refreshWithReusedToken() throws Exception {
        ResultActions loginResultActions = mvc.perform(post("/api/v1/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email":"user1@test.com",
                        "password":"1234"
                    }"""));

        String refreshToken = extractRefreshToken(loginResultActions);

        mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.resultCode").value("401-4"))
            .andExpect(jsonPath("$.msg").value("재사용된 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("토큰 재발급: refreshToken 누락은 400-1이다")
    void refreshWithoutToken() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));

        resultActions.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    private String extractRefreshToken(ResultActions resultActions) throws Exception {
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\\\"refreshToken\\\":\\\"([^\\\"]+)\\\"")
            .matcher(responseBody);

        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
