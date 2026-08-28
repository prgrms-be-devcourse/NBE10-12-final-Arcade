package com.back.domain.member.profiler.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.service.MemberProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestContainerConfig.class)
public class ApiV1MemberProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileService memberProfileService;

    @Test
    @DisplayName("내 정보 조회: 로그인한 회원의 개인정보를 반환한다")
    @WithUserDetails("user1@test.com")
    void me() throws Exception {
        mvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 정보 조회 성공"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.email").value("user1@test.com"))
                .andExpect(jsonPath("$.data.name").value("유저1"))
                .andExpect(jsonPath("$.data.nickname").value("유저1"))
                .andExpect(jsonPath("$.data.webpage").isEmpty())
                .andExpect(jsonPath("$.data.profileImageUrl").isEmpty())
                .andExpect(jsonPath("$.data.positions").isEmpty())
                .andExpect(jsonPath("$.data.techStacks").isEmpty());
    }

    @Test
    @DisplayName("내 정보 수정: positions와 techStacks의 변경분을 반영한다")
    @WithUserDetails("user1@test.com")
    void modifyProfile() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "첫 닉네임",
                                  "webpage": "https://before.example.com",
                                  "profileImageUrl": "before.png",
                                  "positions": ["BACK", "FRONT"],
                                  "techStacks": ["Java", "Spring"]
                                }
                                """))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "새 닉네임",
                                  "webpage": "https://after.example.com",
                                  "profileImageUrl": "after.png",
                                  "positions": ["BACK", "PM"],
                                  "techStacks": ["Java", "Kotlin"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 정보 수정 성공"))
                .andExpect(jsonPath("$.data.nickname").value("새 닉네임"))
                .andExpect(jsonPath("$.data.webpage").value("https://after.example.com"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("after.png"))
                .andExpect(jsonPath("$.data.positions").value(org.hamcrest.Matchers.contains(
                        "BACK",
                        "PM"
                )))
                .andExpect(jsonPath("$.data.techStacks").value(org.hamcrest.Matchers.contains(
                        "Java",
                        "Kotlin"
                )));
    }

    @Test
    @DisplayName("내 정보 수정: 다른 회원이 사용 중인 닉네임이면 409-1을 반환한다")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithDuplicatedNickname() throws Exception {
        memberProfileService.modifyProfile(
                memberRepository.findByEmail("user2@test.com").orElseThrow(),
                "중복 닉네임",
                null,
                null,
                java.util.List.of("BACK"),
                java.util.List.of("Java")
        );

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "중복 닉네임",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("내 정보 수정: 필수값이 누락되면 400-1을 반환한다")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithMissingRequiredFields() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("내 정보 API: 비로그인 요청은 401-1을 반환한다")
    void profileApisRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
