package com.back.domain.interaction.bookmark.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1BookmarkControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContestService contestService;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    private long saveContest() {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        ContestResponseDto contest = contestService.write(
                admin,
                "북마크 테스트용 대회",
                ContestFormat.HACKATHON,
                ContestTag.AI,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "설명",
                "https://example.com/contest",
                null
        );

        return contest.id();
    }

    @Test
    @DisplayName("대회 북마크: 201-1과 bookmarked=true를 반환한다")
    @WithUserDetails("user1@test.com")
    void bookmarkContest() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.targetType").value("CONTEST"))
                .andExpect(jsonPath("$.data.targetId").value(contestId))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("대회 북마크: 로그인하지 않았으면 401-1이다")
    void bookmarkContestWithoutLogin() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("대회 북마크 취소: 로그인하지 않았으면 401-1이다")
    void unbookmarkContestWithoutLogin() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("대회 북마크: 이미 북마크한 대회에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkContestTwice() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("대회 북마크: 존재하지 않는 대회면 404-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkContestNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/contests/999999/bookmarks"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("대회 북마크 취소: 204-1")
    @WithUserDetails("user1@test.com")
    void unbookmarkContest() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("대회 북마크 취소: 북마크하지 않은 대회를 취소하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void unbookmarkContestWithoutBookmarking() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("대회 북마크: 북마크→취소→재북마크를 반복해도 매번 정상 토글된다")
    @WithUserDetails("user1@test.com")
    void bookmarkContestToggleCycle() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        mvc.perform(delete("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    private long saveParty() {
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        Party party = new Party(
                owner,
                "북마크 테스트용 파티",
                "북마크 테스트용 파티 모집",
                "설명",
                null,
                null,
                null,
                TopicType.PROJECT,
                PartyTag.WEB,
                null,
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 2));

        return partyRepository.save(party).getId();
    }

    @Test
    @DisplayName("파티 북마크: 201-1과 bookmarked=true를 반환한다")
    @WithUserDetails("user1@test.com")
    void bookmarkParty() throws Exception {
        long partyId = saveParty();

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + partyId + "/bookmarks"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.targetType").value("PARTY"))
                .andExpect(jsonPath("$.data.targetId").value(partyId))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("파티 북마크: 로그인하지 않았으면 401-1이다")
    void bookmarkPartyWithoutLogin() throws Exception {
        long partyId = saveParty();

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + partyId + "/bookmarks"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("파티 북마크 취소: 로그인하지 않았으면 401-1이다")
    void unbookmarkPartyWithoutLogin() throws Exception {
        long partyId = saveParty();

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + partyId + "/bookmarks"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("파티 북마크: 이미 북마크한 파티에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkPartyTwice() throws Exception {
        long partyId = saveParty();

        mvc.perform(post("/api/v1/parties/" + partyId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + partyId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("파티 북마크: 존재하지 않는 파티면 404-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkPartyNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/parties/999999/bookmarks"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("파티 북마크 취소: 204-1")
    @WithUserDetails("user1@test.com")
    void unbookmarkParty() throws Exception {
        long partyId = saveParty();

        mvc.perform(post("/api/v1/parties/" + partyId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + partyId + "/bookmarks"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("파티 북마크 취소: 북마크하지 않은 파티를 취소하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void unbookmarkPartyWithoutBookmarking() throws Exception {
        long partyId = saveParty();

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + partyId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("파티 북마크: 북마크→취소→재북마크를 반복해도 매번 정상 토글된다")
    @WithUserDetails("user1@test.com")
    void bookmarkPartyToggleCycle() throws Exception {
        long partyId = saveParty();

        mvc.perform(post("/api/v1/parties/" + partyId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        mvc.perform(delete("/api/v1/parties/" + partyId + "/bookmarks"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/parties/" + partyId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    // 자기신고 성취 - 완료(ACHIEVED) 상태라 전시 가능
    private long saveAchievedGoal(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        PersonalChecklist checklist = new PersonalChecklist(
                owner, GoalStatus.ACHIEVED, "자격증 취득", "메모", LocalDate.now()
        );
        return goalRepository.save(checklist).getId();
    }

    // 자기신고 성취 - 아직 진행 중이라 전시 불가
    private long saveInProgressGoal(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        PersonalChecklist checklist = new PersonalChecklist(
                owner, GoalStatus.IN_PROGRESS, "자격증 취득", "메모", LocalDate.now()
        );
        return goalRepository.save(checklist).getId();
    }

    private Party savePartyForGoal(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();

        Party party = new Party(
                owner,
                "오락실 팀",
                "오락실 공모전 팀원 모집",
                "설명",
                null,
                null,
                null,
                TopicType.PROJECT,
                PartyTag.WEB,
                null,
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 2));

        return partyRepository.save(party);
    }

    // PROJECT 성취 - 완료됐지만 파티장이 아직 전시글을 게시하지 않아 전시 불가
    private long saveUnpublishedProjectGoal(String ownerEmail, long partyAssembleToMemberId) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Party party = savePartyForGoal(ownerEmail);

        Project project = new Project(
                owner, partyAssembleToMemberId, party.getId(), "정산 자동화 API", PositionType.BACK, LocalDate.now()
        );
        project.complete(LocalDate.now());

        return goalRepository.save(project).getId();
    }

    // PROJECT 성취 - 완료 + 파티장이 전시글까지 게시해서 전시 가능
    private long savePublishedProjectGoal(String ownerEmail, long partyAssembleToMemberId) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Party party = savePartyForGoal(ownerEmail);

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish("정산 자동화 API", "설명");
        partyShowcaseRepository.save(showcase);

        Project project = new Project(
                owner, partyAssembleToMemberId, party.getId(), "정산 자동화 API", PositionType.BACK, LocalDate.now()
        );
        project.complete(LocalDate.now());
        project.linkShowcase(showcase);

        return goalRepository.save(project).getId();
    }

    @Test
    @DisplayName("성취 북마크: 게시된 PROJECT 전시글이면 201-1과 bookmarked=true를 반환한다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoal() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 1001L);
        long showcaseId = ((Project) goalRepository.findById(goalId).orElseThrow()).getPartyShowcase().getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.targetType").value("PARTY_SHOWCASE"))
                .andExpect(jsonPath("$.data.targetId").value(showcaseId))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("성취 북마크: 자기신고 성취(완료)는 전시·북마크 대상이 아니라 404-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalSelfReported() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 북마크: 아직 진행 중(전시 불가)인 성취면 404-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalNotExhibitedInProgress() throws Exception {
        long goalId = saveInProgressGoal("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 북마크: PROJECT 타입은 완료돼도 전시글이 게시 전이면 404-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalProjectNotPublished() throws Exception {
        long goalId = saveUnpublishedProjectGoal("user2@test.com", 201L);

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 북마크: PROJECT 타입도 전시글이 게시되면 북마크 가능하다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalProjectPublished() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 202L);

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"));
    }

    @Test
    @DisplayName("성취 북마크: 존재하지 않는 성취면 404-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/goals/999999/bookmarks"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 북마크: 이미 북마크한 성취에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalTwice() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 1001L);

        mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("성취 북마크: 로그인하지 않았으면 401-1이다")
    void bookmarkGoalWithoutLogin() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("성취 북마크 취소: 204-1")
    @WithUserDetails("user1@test.com")
    void unbookmarkGoal() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 1001L);

        mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("성취 북마크 취소: 북마크하지 않은 성취를 취소하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void unbookmarkGoalWithoutBookmarking() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 1001L);

        ResultActions resultActions = mvc.perform(delete("/api/v1/goals/" + goalId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("성취 북마크: 북마크→취소→재북마크를 반복해도 매번 정상 토글된다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalToggleCycle() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 1001L);

        mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        mvc.perform(delete("/api/v1/goals/" + goalId + "/bookmarks"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    // 같은 파티의 전시글을 공유하는 팀원 두 명분 Project - 한쪽은 partyAssembleToMemberId만 다르게 준다
    private long[] savePublishedProjectGoalsForTwoMembers(String ownerEmail, String teammateEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Member teammate = memberRepository.findByEmail(teammateEmail).orElseThrow();
        Party party = savePartyForGoal(ownerEmail);

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish("정산 자동화 API", "설명");
        partyShowcaseRepository.save(showcase);

        Project ownerProject = new Project(
                owner, 301L, party.getId(), "정산 자동화 API", PositionType.BACK, LocalDate.now());
        ownerProject.complete(LocalDate.now());
        ownerProject.linkShowcase(showcase);

        Project teammateProject = new Project(
                teammate, 302L, party.getId(), "정산 자동화 API", PositionType.FRONT, LocalDate.now());
        teammateProject.complete(LocalDate.now());
        teammateProject.linkShowcase(showcase);

        return new long[]{
                goalRepository.save(ownerProject).getId(),
                goalRepository.save(teammateProject).getId()
        };
    }

    @Test
    @DisplayName("성취 북마크: PROJECT는 GOAL이 아니라 PARTY_SHOWCASE 대상으로 실제 저장된다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalProjectRoutesToPartyShowcase() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 203L);
        Project project = (Project) goalRepository.findById(goalId).orElseThrow();
        long showcaseId = project.getPartyShowcase().getId();

        mvc.perform(post("/api/v1/goals/" + goalId + "/bookmarks"))
                .andExpect(status().isCreated());

        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        assertThat(bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(
                actor, TargetType.PARTY_SHOWCASE, showcaseId)).isTrue();
    }

    @Test
    @DisplayName("성취 북마크: 같은 전시글을 공유하는 팀원의 다른 goalId를 북마크하면 같은 대상이라 409-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkGoalProjectSharesTargetAcrossTeammates() throws Exception {
        long[] goalIds = savePublishedProjectGoalsForTwoMembers("user2@test.com", "user3@test.com");

        mvc.perform(post("/api/v1/goals/" + goalIds[0] + "/bookmarks"))
                .andExpect(status().isCreated());

        // 다른 goalId지만 같은 파티의 같은 전시글을 가리키므로 이미 북마크한 것으로 취급돼야 한다.
        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalIds[1] + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }
}
