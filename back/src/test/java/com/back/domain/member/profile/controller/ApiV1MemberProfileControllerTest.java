package com.back.domain.member.profile.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.service.MemberProfileService;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Test
    @DisplayName("내 활동 요약: 활동이 없으면 전부 0 이다")
    @WithUserDetails("user1@test.com")
    void summaryWithoutActivity() throws Exception {
        mvc.perform(get("/api/v1/members/me/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.completedParties").value(0))
                .andExpect(jsonPath("$.data.awards").value(0))
                .andExpect(jsonPath("$.data.exhibitions").value(0))
                .andExpect(jsonPath("$.data.streakDays").value(0))
                .andExpect(jsonPath("$.data.activityHeatmap.length()").value(56))
                .andExpect(jsonPath("$.data.badges").isEmpty());
    }

    @Test
    @DisplayName("내 활동 요약: 파티 활동이 있으면 스트릭과 히트맵에 반영된다")
    @WithUserDetails("user1@test.com")
    void summaryReflectsActivity() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // 파티 지원은 기획서 2.9 의 활동 목록에 들어간다 - 오늘 자 기록이 남아야 한다
        Party party = partyRepository.save(newParty(owner));
        mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"positionId\": %d }".formatted(
                                party.getPositions().getFirst().getId())))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/members/me/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.streakDays").value(1))
                // 히트맵은 오래된 날부터라 오늘이 마지막 칸이다
                .andExpect(jsonPath("$.data.activityHeatmap[55]").value(1))
                .andExpect(jsonPath("$.data.activityHeatmap[54]").value(0));
    }

    @Test
    @DisplayName("내 활동 요약: 승인된 완료 파티만 세고, 거절되거나 진행 중인 건은 빼고 센다")
    @WithUserDetails("user1@test.com")
    void summary() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // 승인 + 완료 + 전시 게시 -> completedParties 1, exhibitions 1
        Party completed = partyRepository.save(completedParty(owner));
        partyMemberRepository.save(partyMember(completed, actor, true));
        PartyShowcase showcase = new PartyShowcase(completed);
        showcase.publish("전시 제목", "설명");
        partyShowcaseRepository.save(showcase);

        // 완료됐지만 거절당한 파티 - 상태 조건이 빠지면 여기서 수가 늘어난다
        Party rejected = partyRepository.save(completedParty(owner));
        partyMemberRepository.save(partyMember(rejected, actor, false));
        PartyShowcase rejectedShowcase = new PartyShowcase(rejected);
        rejectedShowcase.publish("남의 전시", "설명");
        partyShowcaseRepository.save(rejectedShowcase);

        // 승인됐지만 아직 모집중인 파티 - 파티 상태 조건이 빠지면 여기서 수가 늘어난다
        Party recruiting = partyRepository.save(newParty(owner));
        partyMemberRepository.save(partyMember(recruiting, actor, true));

        goalRepository.save(new PersonalContest(
                actor, GoalStatus.ACHIEVED, "공모전 대상", false, "대상", LocalDate.now(), null));
        // 아직 달성 전인 수상 - 상태 조건이 빠지면 여기서 수가 늘어난다
        goalRepository.save(new PersonalContest(
                actor, GoalStatus.WANT, "지원 예정 공모전", false, null, null, null));

        mvc.perform(get("/api/v1/members/me/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedParties").value(1))
                .andExpect(jsonPath("$.data.awards").value(1))
                .andExpect(jsonPath("$.data.exhibitions").value(1));
    }

    private Party completedParty(Member owner) {
        Party party = newParty(owner);
        party.closeRecruiting();
        party.complete();

        return party;
    }

    private Party newParty(Member owner) {
        Party party = new Party(
                owner, "파티", "제목", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null,
                LocalDateTime.now().plusDays(7));
        party.addPosition(new Position(PositionType.BACK, 3));

        return party;
    }

    private PartyMember partyMember(Party party, Member member, boolean approved) {
        PartyMember partyMember = new PartyMember(party, member, party.getPositions().getFirst(), null);

        if (approved) partyMember.approve();
        else partyMember.reject();

        return partyMember;
    }

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
                .andExpect(jsonPath("$.data.position").doesNotExist())
                .andExpect(jsonPath("$.data.techStacks").isEmpty());
    }

    @Test
    @DisplayName("내 정보 수정: position과 techStacks의 변경분을 반영한다")
    @WithUserDetails("user1@test.com")
    void modifyProfile() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "첫 닉네임",
                                  "webpage": "https://before.example.com",
                                  "profileImageUrl": "before.png",
                                  "position": "BACK",
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
                                  "position": "PM",
                                  "techStacks": ["Java", "Kotlin"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 정보 수정 성공"))
                .andExpect(jsonPath("$.data.nickname").value("새 닉네임"))
                .andExpect(jsonPath("$.data.webpage").value("https://after.example.com"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("after.png"))
                .andExpect(jsonPath("$.data.position").value("PM"))
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
                                  "position": "BACK",
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
                                  "position": "BACK",
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
                                  "position": "BACK",
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
                                  "position": "BACK",
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
                                  "position": "BACK",
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
                                  "position": "BACK",
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
                                  "position": "BACK",
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
                com.back.domain.member.member.entity.PositionType.BACK,
                java.util.List.of("Java"),
                java.util.List.of(),
                java.util.List.of()
        );

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "중복 닉네임",
                                  "position": "BACK",
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
    @DisplayName("내 정보 수정: 정의되지 않은 직군이면 400-2 이다 (본문 파싱 실패)")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithUnknownPosition() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "유효하지 않은 직군 테스트",
                                  "position": "adbc",
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @DisplayName("내 정보 수정: position 을 생략하면 비운다")
    @WithUserDetails("user1@test.com")
    void modifyProfileClearsPositionWhenOmitted() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "직군 있는 사람",
                                  "position": "BACK",
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.position").value("BACK"));

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "직군 없는 사람",
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isOk())
                // @JsonInclude(NON_NULL) 이라 비면 필드 자체가 빠진다
                .andExpect(jsonPath("$.data.position").doesNotExist());
    }

    @Test
    @DisplayName("내 정보 수정: 닉네임 말고 다 생략하면 나머지가 비워진다 (보낸 값이 곧 저장될 값)")
    @WithUserDetails("user1@test.com")
    void modifyProfileClearsOmittedFields() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "다 채운 사람",
                                  "bio": "소개글",
                                  "position": "BACK",
                                  "techStacks": ["Java", "Spring"],
                                  "careers": [{ "role": "백엔드 개발" }],
                                  "links": [{ "label": "GitHub", "url": "https://github.com/x" }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.techStacks", org.hamcrest.Matchers.hasSize(2)));

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nickname": "다 비운 사람" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").doesNotExist())
                .andExpect(jsonPath("$.data.position").doesNotExist())
                .andExpect(jsonPath("$.data.techStacks", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data.careers", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data.links", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("내 정보 수정: techStacks 의 null 요소는 400-1을 반환한다")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithNullCollectionElement() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "기술 null 테스트",
                                  "position": "BACK",
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

    @Test
    @DisplayName("프로필 이미지 업로드: 저장한 이미지의 URL을 반환한다")
    @WithUserDetails("user1@test.com")
    void uploadProfileImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "fake-png".getBytes());

        mvc.perform(multipart("/api/v1/members/me/image").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("프로필 이미지 업로드 성공"))
                .andExpect(jsonPath("$.data.profileImageUrl").value(
                        org.hamcrest.Matchers.matchesPattern("^/uploads/profile/[0-9a-f-]{36}\\.png$")));
    }

    @Test
    @DisplayName("프로필 이미지 업로드: 이미지가 아니면 400-1")
    @WithUserDetails("user1@test.com")
    void uploadProfileImageWithWrongType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "not-an-image".getBytes());

        mvc.perform(multipart("/api/v1/members/me/image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("프로필 이미지 업로드: 화면이 막는 gif 는 서버도 막는다")
    @WithUserDetails("user1@test.com")
    void uploadProfileImageWithGif() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.gif", MediaType.IMAGE_GIF_VALUE, "fake-gif".getBytes());

        mvc.perform(multipart("/api/v1/members/me/image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("프로필 이미지 업로드: Content-Type 이 없어도 500 이 아니라 400-1")
    @WithUserDetails("user1@test.com")
    void uploadProfileImageWithoutContentType() throws Exception {
        // Content-Type 헤더를 안 붙인 파트다. getContentType() 이 null 로 오는데,
        // List.of() 목록에 그대로 contains 하면 NPE 로 500 이 난다.
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", null, "fake-png".getBytes());

        mvc.perform(multipart("/api/v1/members/me/image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("프로필 이미지 업로드: 빈 파일이면 400-1")
    @WithUserDetails("user1@test.com")
    void uploadEmptyProfileImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);

        mvc.perform(multipart("/api/v1/members/me/image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("프로필 이미지 업로드: 미로그인이면 401")
    void uploadProfileImageWithoutLogin() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "fake-png".getBytes());

        mvc.perform(multipart("/api/v1/members/me/image").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("포지션만 수정: 다른 항목은 건드리지 않고 대표 포지션만 바꾼다")
    @WithUserDetails("user1@test.com")
    void modifyPositionOnly() throws Exception {
        mvc.perform(patch("/api/v1/members/me/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "position": "UIUX" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("대표 포지션 수정 성공"))
                .andExpect(jsonPath("$.data.position").value("UIUX"));
    }

    @Test
    @DisplayName("포지션만 수정: position 이 없으면 400-1")
    @WithUserDetails("user1@test.com")
    void modifyPositionWithoutValue() throws Exception {
        mvc.perform(patch("/api/v1/members/me/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("포지션만 수정: 미로그인이면 401")
    void modifyPositionWithoutLogin() throws Exception {
        mvc.perform(patch("/api/v1/members/me/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "position": "BACK" }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
