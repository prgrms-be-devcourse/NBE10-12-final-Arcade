package com.back.domain.member.profile.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.service.MemberProfileService;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.assemble.entity.PartyAssemble;
import com.back.domain.party.assemble.entity.PartyAssembleToMember;
import com.back.domain.party.assemble.repository.PartyAssembleRepository;
import com.back.domain.party.assemble.repository.PartyAssembleToMemberRepository;
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
    private PartyAssembleRepository partyAssembleRepository;

    @Autowired
    private PartyAssembleToMemberRepository partyAssembleToMemberRepository;

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
                .andExpect(jsonPath("$.data.badges").isEmpty())
                // '크루온 활동 N개월째' 는 가입일부터 센다 - 활동이 없어도 값이 있어야 한다
                .andExpect(jsonPath("$.data.joinedAt").exists());
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
    @DisplayName("내 활동 요약: 확정 명단에 든 완료 파티만 세고, 확정에서 빠졌거나 진행 중인 건은 빼고 센다")
    @WithUserDetails("user1@test.com")
    void summary() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // 확정 명단 + 완료 + 전시 게시 -> completedParties 1, exhibitions 1
        Party completed = partyRepository.save(completedParty(owner));
        assemble(completed, actor);
        publishShowcase(completed, "전시 제목");

        // 완료됐지만 내가 확정 명단에 없는 파티 - 명단 조건이 빠지면 여기서 수가 늘어난다
        Party notMine = partyRepository.save(completedParty(owner));
        assemble(notMine, owner);
        publishShowcase(notMine, "남의 전시");

        // 확정 명단에 들었지만 아직 진행 중인 파티 - 파티 상태 조건이 빠지면 여기서 수가 늘어난다
        Party inProgress = partyRepository.save(assembledParty(owner));
        assemble(inProgress, actor);

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

    @Test
    @DisplayName("내 활동 요약: 내가 파티장인 파티도 완료·전시 건수에 잡힌다")
    @WithUserDetails("user1@test.com")
    void summaryCountsPartiesIOwn() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();

        // 파티장은 PartyMember 로 남지 않는다 - 확정 명단만 보고 세야 잡힌다
        Party myParty = partyRepository.save(completedParty(actor));
        assemble(myParty, actor);
        publishShowcase(myParty, "내가 연 전시");

        mvc.perform(get("/api/v1/members/me/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedParties").value(1))
                .andExpect(jsonPath("$.data.exhibitions").value(1));
    }

    /** 확정 명단에 한 명 올린다. 실제 마감(closeRecruiting)이 남기는 기록과 같은 모양. */
    private void assemble(Party party, Member member) {
        PartyAssemble partyAssemble = partyAssembleRepository.save(new PartyAssemble(party));
        partyAssembleToMemberRepository.save(new PartyAssembleToMember(partyAssemble, member));
    }

    private void publishShowcase(Party party, String title) {
        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish(title, "설명");
        partyShowcaseRepository.save(showcase);
    }

    private Party completedParty(Member owner) {
        Party party = newParty(owner);
        party.closeRecruiting();
        party.complete();

        return party;
    }

    private Party assembledParty(Member owner) {
        Party party = newParty(owner);
        party.closeRecruiting();

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
    @DisplayName("내 정보 수정: 경력·링크는 목록을 보내면 통째로 교체하고, 빈 배열이면 지워진다")
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
    @DisplayName("내 정보 수정: careers·links 를 생략하면 건드리지 않는다")
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
    @DisplayName("내 정보 수정: profileImageUrl 을 빼면 올린 이미지가 그대로 남고, \"\" 를 보내야 지워진다")
    @WithUserDetails("user1@test.com")
    void keepsUploadedImageUnlessClearedExplicitly() throws Exception {
        // 업로드(POST /me/image)로 받은 URL 을 실어 보내는 흐름
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "profileImageUrl": "https://storage.example.com/me.png" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://storage.example.com/me.png"));

        // 사진을 안 바꾼 저장 - 필드를 빼면 그대로 남는다
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "bio": "사진은 그대로" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://storage.example.com/me.png"));

        // 기본 아바타로 되돌리기 - 빈 문자열을 명시해야 지워진다
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "profileImageUrl": "" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());
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
    @DisplayName("내 정보 수정: 닉네임이 공백뿐이면 400-1을 반환한다 (전각 공백 포함)")
    @WithUserDetails("user1@test.com")
    void modifyProfileWithBlankNickname() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nickname": "  " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nickname\": \"\u3000\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("내 정보 수정: 닉네임 앞뒤 공백은 잘라서 저장한다")
    @WithUserDetails("user1@test.com")
    void modifyProfileTrimsNickname() throws Exception {
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nickname": "  공백낀사람  " }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("공백낀사람"));
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
    @DisplayName("내 정보 수정: position 을 생략하면 그대로 둔다")
    @WithUserDetails("user1@test.com")
    void modifyProfileKeepsPositionWhenOmitted() throws Exception {
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
                                  "nickname": "직군 그대로인 사람",
                                  "techStacks": ["Java"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.position").value("BACK"));
    }

    @Test
    @DisplayName("내 정보 수정: 생략한 항목은 그대로 두고, 빈 값을 보낸 항목만 비운다")
    @WithUserDetails("user1@test.com")
    void modifyProfileKeepsOmittedFieldsAndClearsEmptyOnes() throws Exception {
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

        // 닉네임만 보낸다 - 화면이 다루지 않는 나머지는 그대로 있어야 한다
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nickname": "닉네임만 바꾼 사람" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("닉네임만 바꾼 사람"))
                .andExpect(jsonPath("$.data.bio").value("소개글"))
                .andExpect(jsonPath("$.data.position").value("BACK"))
                .andExpect(jsonPath("$.data.techStacks", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.careers", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.links", org.hamcrest.Matchers.hasSize(1)));

        // 비우는 건 빈 값을 명시했을 때만이다
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bio": "",
                                  "techStacks": [],
                                  "careers": [],
                                  "links": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("닉네임만 바꾼 사람"))
                .andExpect(jsonPath("$.data.bio").doesNotExist())
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
    @DisplayName("내 정보 수정: 닉네임이 없는 회원도 position 만 보내 저장할 수 있다 (GitHub 최초 가입 흐름)")
    @WithUserDetails("user1@test.com")
    void modifyPositionOnlyWithoutNickname() throws Exception {
        // GitHub 로 가입하면 닉네임이 없다. 그 상태에서 포지션만 고르는 화면이 쓰는 요청이다.
        mvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "position": "UIUX" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.position").value("UIUX"))
                .andExpect(jsonPath("$.data.nickname").doesNotExist());
    }

    @Test
    @DisplayName("공개 프로필: 로그인 없이 열리고 email 은 내려주지 않는다")
    void publicProfileWithoutLogin() throws Exception {
        Member target = memberRepository.findByEmail("user1@test.com").orElseThrow();

        mvc.perform(get("/api/v1/members/" + target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                // 본인 화면 전용 값이라 공개 응답에는 없다
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.githubLinked").doesNotExist())
                // 프로필을 한 번도 저장한 적 없어도 404 가 아니다
                .andExpect(jsonPath("$.data.techStacks").isEmpty())
                .andExpect(jsonPath("$.data.streakDays").value(0))
                .andExpect(jsonPath("$.data.activityHeatmap.length()").value(56))
                .andExpect(jsonPath("$.data.joinedAt").exists());
    }

    @Test
    @DisplayName("공개 프로필: 연속 활동일·히트맵과 수상 성취가 함께 실린다")
    @WithUserDetails("user1@test.com")
    void publicProfileCarriesStreakAndAchievements() throws Exception {
        Member target = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // 파티 지원은 기획서 2.9 의 활동이라 오늘 자 기록이 남는다
        Party party = partyRepository.save(newParty(owner));
        mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"positionId\": %d }".formatted(
                                party.getPositions().getFirst().getId())))
                .andExpect(status().isCreated());

        goalRepository.save(new PersonalContest(
                target, GoalStatus.ACHIEVED, "공모전 대상", false, "대상", LocalDate.now(), null));
        // 아직 달성 전 - 목록·건수 모두에서 빠져야 한다
        goalRepository.save(new PersonalContest(
                target, GoalStatus.WANT, "지원 예정 공모전", false, null, null, null));

        mvc.perform(get("/api/v1/members/" + target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.streakDays").value(1))
                // 히트맵은 오래된 날부터라 오늘이 마지막 칸이다
                .andExpect(jsonPath("$.data.activityHeatmap[55]").value(1))
                .andExpect(jsonPath("$.data.awards").value(1))
                .andExpect(jsonPath("$.data.achievements.length()").value(1))
                .andExpect(jsonPath("$.data.achievements[0].detail.title").value("공모전 대상"));
    }

    @Test
    @DisplayName("공개 프로필: 성취는 달성한 것만, 타입은 가리지 않는다(기획서 3.7)")
    @WithUserDetails("user1@test.com")
    void publicProfileCarriesCompletedProjectsOnly() throws Exception {
        Member target = memberRepository.findByEmail("user1@test.com").orElseThrow();

        // 파티 확정 때 참여자 전원에게 생기고, 파티 완료 때 ACHIEVED 가 된다
        Project done = new Project(
                target, null, 1L, "완료한 파티", PositionType.BACK, LocalDate.now().minusMonths(2));
        done.complete(LocalDate.now());
        goalRepository.save(done);

        // 아직 진행 중인 파티 - 상태 조건이 빠지면 여기서 수가 늘어난다
        goalRepository.save(new Project(
                target, null, 2L, "진행 중인 파티", PositionType.BACK, LocalDate.now()));

        // 체크리스트도 성취다 - 기획서 3.7 이 공개 여부 필드를 두지 않기로 해 타입으로 거르지 않는다
        goalRepository.save(new PersonalChecklist(
                target, GoalStatus.ACHIEVED, "개인 목표", "메모", LocalDate.now()));

        mvc.perform(get("/api/v1/members/" + target.getId()))
                .andExpect(status().isOk())
                // 완료한 PROJECT + 체크리스트 = 2건. 진행 중인 파티만 빠진다
                .andExpect(jsonPath("$.data.achievements.length()").value(2))
                .andExpect(jsonPath("$.data.achievements[?(@.type == 'PROJECT')].detail.title")
                        .value("완료한 파티"))
                .andExpect(jsonPath("$.data.achievements[?(@.type == 'PROJECT')].detail.exhibited")
                        .value(false))
                .andExpect(jsonPath("$.data.achievements[?(@.type == 'CHECKLIST')].detail.title")
                        .value("개인 목표"));
    }

    @Test
    @DisplayName("공개 프로필: 없는 회원이면 404")
    @WithUserDetails("user1@test.com")
    void publicProfileNotFound() throws Exception {
        mvc.perform(get("/api/v1/members/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
