package com.back.domain.member.profile.controller;

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
                .andExpect(jsonPath("$.data.nickname").isEmpty())
                .andExpect(jsonPath("$.data.webpage").isEmpty())
                .andExpect(jsonPath("$.data.profileImageUrl").isEmpty())
                .andExpect(jsonPath("$.data.githubLinked").value(false))
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
    @DisplayName("내 정보 수정: 소개·경력·링크가 저장된다")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithBioCareersAndLinks() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "소개쓰는사람",
                                  "bio": "백엔드를 주로 합니다.",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"],
                                  "careers": [
                                    { "startDate": "2024-03-01", "endDate": null, "role": "백엔드 개발", "org": "오락실", "description": "결제 모듈" },
                                    { "startDate": "2023-01-01", "endDate": "2024-02-28", "role": "인턴", "org": "크루온", "description": null }
                                  ],
                                  "links": [
                                    { "label": "GitHub", "url": "https://github.com/haneul-dev" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("백엔드를 주로 합니다."))
                .andExpect(jsonPath("$.data.careers", org.hamcrest.Matchers.hasSize(2)))
                // 보낸 순서 그대로 온다
                .andExpect(jsonPath("$.data.careers[0].role").value("백엔드 개발"))
                // endDate 를 안 보내면 재직중이라 응답에서 빠진다(NON_NULL)
                .andExpect(jsonPath("$.data.careers[0].endDate").doesNotExist())
                .andExpect(jsonPath("$.data.careers[0].org").value("오락실"))
                .andExpect(jsonPath("$.data.careers[1].role").value("인턴"))
                .andExpect(jsonPath("$.data.careers[1].endDate").value("2024-02-28"))
                .andExpect(jsonPath("$.data.links", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.links[0].label").value("GitHub"));
    }

    @Test
    @DisplayName("내 정보 수정: 경력·링크는 보낸 목록이 곧 저장될 목록이라 빈 배열을 보내면 지워진다")
    @WithUserDetails("user1@test.com")
    void modifyProfileReplacesCareersAndLinks() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "경력있는사람",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"],
                                  "careers": [{ "role": "백엔드 개발", "org": "오락실" }],
                                  "links": [{ "label": "GitHub", "url": "https://github.com/x" }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.careers", org.hamcrest.Matchers.hasSize(1)));

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "경력있는사람",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"],
                                  "careers": [],
                                  "links": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.careers", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data.links", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("내 정보 수정: 제목 없는 경력과 주소 없는 링크는 저장하지 않는다")
    @WithUserDetails("user1@test.com")
    void modifyProfileSkipsIncompleteRows() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "빈칸있는사람",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"],
                                  "careers": [
                                    { "startDate": "2024-03-01", "role": "  ", "org": "오락실" },
                                    { "role": "제대로 적은 경력" }
                                  ],
                                  "links": [
                                    { "label": "블로그", "url": "" },
                                    { "label": "GitHub", "url": "https://github.com/x" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.careers", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.careers[0].role").value("제대로 적은 경력"))
                .andExpect(jsonPath("$.data.links", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.links[0].label").value("GitHub"));
    }

    @Test
    @DisplayName("내 정보 수정: careers·links 를 생략해도 400 이 아니다")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithoutCareersAndLinks() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "생략하는사람",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.careers", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data.links", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("내 정보 수정: 직접 올린 이미지와 GitHub 아바타를 합치지 않고 따로 내려준다")
    @WithUserDetails("user1@test.com")
    void keepsUploadedImageAndGithubAvatarSeparate() throws Exception {
        // OAuth 로그인이 채워두는 값을 흉내낸다
        memberRepository.findByEmail("user1@test.com").orElseThrow()
                .setProfileImgUrl("https://avatars.githubusercontent.com/u/1");

        // 아직 직접 올린 게 없으면 그 자리는 비고, 아바타만 온다
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "이미지없는사람",
                                  "profileImageUrl": null,
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.githubAvatarUrl")
                        .value("https://avatars.githubusercontent.com/u/1"));

        // 직접 올리면 둘이 각각 온다 - 서버가 합치지 않으므로 아바타가 덮이지 않는다
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "이미지올린사람",
                                  "profileImageUrl": "https://storage.example.com/me.png",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://storage.example.com/me.png"))
                .andExpect(jsonPath("$.data.githubAvatarUrl")
                        .value("https://avatars.githubusercontent.com/u/1"));
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
                null,
                java.util.List.of("BACK"),
                java.util.List.of("Java"),
                java.util.List.of(),
                java.util.List.of()
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
    @DisplayName("내 정보 수정: 유효하지 않은 직군은 저장하지 않는다")
    @WithUserDetails("user1@test.com")
    void modifyProfileIgnoresInvalidPosition() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "유효하지 않은 직군 테스트",
                                  "positions": ["BACK", "adbc"],
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positions").value(org.hamcrest.Matchers.contains("BACK")));
    }

    @Test
    @DisplayName("내 정보 수정: positions 또는 techStacks의 null 요소는 400-1을 반환한다")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithNullCollectionElement() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "직군 null 테스트",
                                  "positions": ["BACK", null],
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "기술 null 테스트",
                                  "positions": ["BACK"],
                                  "techStacks": ["Java", null]
                                }
                                """))
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
