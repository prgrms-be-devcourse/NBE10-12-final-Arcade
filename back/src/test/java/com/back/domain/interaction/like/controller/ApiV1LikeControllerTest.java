package com.back.domain.interaction.like.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1LikeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private ContestService contestService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    private long saveContest() {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        ContestResponseDto contest = contestService.write(
                admin,
                "좋아요 테스트용 대회",
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

    private Party saveParty(String ownerEmail) {
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

    private long saveAchievedGoal(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        PersonalChecklist checklist = new PersonalChecklist(
                owner, GoalStatus.ACHIEVED, "자격증 취득", "메모", LocalDate.now()
        );
        return goalRepository.save(checklist).getId();
    }

    private long saveInProgressGoal(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        PersonalChecklist checklist = new PersonalChecklist(
                owner, GoalStatus.IN_PROGRESS, "자격증 취득", "메모", LocalDate.now()
        );
        return goalRepository.save(checklist).getId();
    }

    private long saveUnpublishedProjectGoal(String ownerEmail, long partyAssembleToMemberId) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Party party = saveParty(ownerEmail);

        Project project = new Project(
                owner, partyAssembleToMemberId, party.getId(), "정산 자동화 API", PositionType.BACK, LocalDate.now()
        );
        project.complete(LocalDate.now());

        return goalRepository.save(project).getId();
    }

    private long savePublishedProjectGoal(String ownerEmail, long partyAssembleToMemberId) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Party party = saveParty(ownerEmail);

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

    private record TeamProjectGoals(long goalIdA, long goalIdB) {
    }

    private TeamProjectGoals savePublishedProjectGoalsSharingShowcase(String memberAEmail, String memberBEmail) {
        Party party = saveParty(memberAEmail);

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish("정산 자동화 API", "설명");
        partyShowcaseRepository.save(showcase);

        Member memberA = memberRepository.findByEmail(memberAEmail).orElseThrow();
        Project projectA = new Project(
                memberA, 501L, party.getId(), "정산 자동화 API", PositionType.BACK, LocalDate.now()
        );
        projectA.complete(LocalDate.now());
        projectA.linkShowcase(showcase);
        long goalIdA = goalRepository.save(projectA).getId();

        Member memberB = memberRepository.findByEmail(memberBEmail).orElseThrow();
        Project projectB = new Project(
                memberB, 502L, party.getId(), "정산 자동화 API", PositionType.FRONT, LocalDate.now()
        );
        projectB.complete(LocalDate.now());
        projectB.linkShowcase(showcase);
        long goalIdB = goalRepository.save(projectB).getId();

        return new TeamProjectGoals(goalIdA, goalIdB);
    }

    @Test
    @DisplayName("파티 좋아요: 201-1과 liked=true, likeCount 증가를 반환한다")
    @WithUserDetails("user1@test.com")
    void likeParty() throws Exception {
        Party party = saveParty("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/likes"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.targetType").value("PARTY"))
                .andExpect(jsonPath("$.data.targetId").value(party.getId()))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("파티 좋아요: 이미 좋아요한 파티에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void likePartyTwice() throws Exception {
        Party party = saveParty("user2@test.com");

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/likes"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("파티 좋아요: 존재하지 않는 파티면 404-1이다")
    @WithUserDetails("user1@test.com")
    void likePartyNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/parties/999999/likes"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("파티 좋아요 취소: 204-1")
    @WithUserDetails("user1@test.com")
    void unlikeParty() throws Exception {
        Party party = saveParty("user2@test.com");

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + party.getId() + "/likes"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("파티 좋아요 취소: 좋아요하지 않은 파티를 취소하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void unlikePartyWithoutLiking() throws Exception {
        Party party = saveParty("user2@test.com");

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + party.getId() + "/likes"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("대회 좋아요: 201-1과 liked=true, likeCount 증가를 반환한다")
    @WithUserDetails("user1@test.com")
    void likeContest() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/likes"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.targetType").value("CONTEST"))
                .andExpect(jsonPath("$.data.targetId").value(contestId))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("대회 좋아요: 이미 좋아요한 대회에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void likeContestTwice() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/likes"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("대회 좋아요: 로그인하지 않았으면 401-1이다")
    void likeContestWithoutLogin() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/likes"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("대회 좋아요 취소: 로그인하지 않았으면 401-1이다")
    void unlikeContestWithoutLogin() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/likes"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("대회 좋아요: 존재하지 않는 대회면 404-1이다")
    @WithUserDetails("user1@test.com")
    void likeContestNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/contests/999999/likes"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("대회 좋아요 취소: 204-1")
    @WithUserDetails("user1@test.com")
    void unlikeContest() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/likes"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("대회 좋아요 취소: 좋아요하지 않은 대회를 취소하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void unlikeContestWithoutLiking() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/likes"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("대회 좋아요: 좋아요→취소→재좋아요를 반복해도 likeCount가 1↔0으로 정확히 오간다")
    @WithUserDetails("user1@test.com")
    void likeContestToggleCycleKeepsCountConsistent() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/likes"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mvc.perform(delete("/api/v1/contests/" + contestId + "/likes"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/contests/" + contestId + "/likes"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("성취 좋아요: 201-1과 liked=true, likeCount 증가를 반환한다 (자기신고, 완료 상태)")
    @WithUserDetails("user1@test.com")
    void likeGoal() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.targetType").value("GOAL"))
                .andExpect(jsonPath("$.data.targetId").value(goalId))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("성취 좋아요: 아직 진행 중(전시 불가)인 성취면 404-1이다")
    @WithUserDetails("user1@test.com")
    void likeGoalNotExhibitedInProgress() throws Exception {
        long goalId = saveInProgressGoal("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 좋아요: PROJECT 타입은 완료돼도 전시글이 게시 전이면 404-1이다")
    @WithUserDetails("user1@test.com")
    void likeGoalProjectNotPublished() throws Exception {
        long goalId = saveUnpublishedProjectGoal("user2@test.com", 101L);

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 좋아요: PROJECT 타입도 전시글이 게시되면 좋아요 가능하다")
    @WithUserDetails("user1@test.com")
    void likeGoalProjectPublished() throws Exception {
        long goalId = savePublishedProjectGoal("user2@test.com", 102L);

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"));
    }

    @Test
    @DisplayName("성취 좋아요: 존재하지 않는 성취면 404-1이다")
    @WithUserDetails("user1@test.com")
    void likeGoalNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/goals/999999/likes"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 좋아요: 이미 좋아요한 성취에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void likeGoalTwice() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        mvc.perform(post("/api/v1/goals/" + goalId + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("성취 좋아요: 로그인하지 않았으면 401-1이다")
    void likeGoalWithoutLogin() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("성취 좋아요 취소: 204-1")
    @WithUserDetails("user1@test.com")
    void unlikeGoal() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        mvc.perform(post("/api/v1/goals/" + goalId + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("성취 좋아요 취소: 좋아요하지 않은 성취를 취소하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void unlikeGoalWithoutLiking() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        ResultActions resultActions = mvc.perform(delete("/api/v1/goals/" + goalId + "/likes"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("성취 좋아요: 좋아요→취소→재좋아요를 반복해도 likeCount가 1↔0으로 정확히 오간다")
    @WithUserDetails("user1@test.com")
    void likeGoalToggleCycleKeepsCountConsistent() throws Exception {
        long goalId = saveAchievedGoal("user2@test.com");

        mvc.perform(post("/api/v1/goals/" + goalId + "/likes"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mvc.perform(delete("/api/v1/goals/" + goalId + "/likes"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/goals/" + goalId + "/likes"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("성취 좋아요: 같은 파티 팀원 A, B의 PROJECT 성취는 같은 전시글(PARTY_SHOWCASE)로 라우팅되어, A를 좋아요한 뒤 B에 좋아요하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void likeTeammateGoalConflictsWithSameShowcase() throws Exception {
        TeamProjectGoals goals = savePublishedProjectGoalsSharingShowcase("user2@test.com", "admin");

        mvc.perform(post("/api/v1/goals/" + goals.goalIdA() + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goals.goalIdB() + "/likes"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("성취 좋아요: 팀원 A의 성취 좋아요를 취소하면, 같은 전시글을 가리키는 팀원 B의 성취에 다시 좋아요할 수 있다")
    @WithUserDetails("user1@test.com")
    void unlikeTeammateGoalAllowsLikingOtherTeammateGoal() throws Exception {
        TeamProjectGoals goals = savePublishedProjectGoalsSharingShowcase("user2@test.com", "admin");

        mvc.perform(post("/api/v1/goals/" + goals.goalIdA() + "/likes"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/goals/" + goals.goalIdA() + "/likes"))
                .andExpect(status().isNoContent());

        ResultActions resultActions = mvc.perform(post("/api/v1/goals/" + goals.goalIdB() + "/likes"));

        resultActions.andExpect(status().isCreated());
    }
}
