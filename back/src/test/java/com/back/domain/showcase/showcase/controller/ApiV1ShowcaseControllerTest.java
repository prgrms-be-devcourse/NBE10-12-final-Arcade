package com.back.domain.showcase.showcase.controller;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1ShowcaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    // 자기신고 - 완료(전시 대상)
    private long saveAchievedChecklist(String ownerEmail, String title) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        PersonalChecklist checklist = new PersonalChecklist(
                owner, GoalStatus.ACHIEVED, title, "메모", LocalDate.now()
        );
        return goalRepository.save(checklist).getId();
    }

    // 자기신고 - 진행 중(전시 대상 아님)
    private long saveInProgressChecklist(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        PersonalChecklist checklist = new PersonalChecklist(
                owner, GoalStatus.IN_PROGRESS, "진행 중인 목표", "메모", LocalDate.now()
        );
        return goalRepository.save(checklist).getId();
    }

    private long saveAchievedContest(String ownerEmail, String title) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        PersonalContest contest = new PersonalContest(
                owner, GoalStatus.ACHIEVED, title, true, "대상", LocalDate.now(), "https://example.com"
        );
        return goalRepository.save(contest).getId();
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
                1,
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 2));

        return partyRepository.save(party);
    }

    // PROJECT - 완료 + 전시글 게시까지 완료 (전시 대상)
    private long savePublishedProject(String ownerEmail, String title, long partyAssembleToMemberId) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Party party = saveParty(ownerEmail);

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish(title, "설명");
        partyShowcaseRepository.save(showcase);

        Project project = new Project(
                owner, partyAssembleToMemberId, party.getId(), title, PositionType.BACK, LocalDate.now()
        );
        project.complete(LocalDate.now());
        project.linkShowcase(showcase);

        return goalRepository.save(project).getId();
    }

    // PROJECT - 완료됐지만 전시글 미게시 (전시 대상 아님)
    private long saveUnpublishedProject(String ownerEmail, long partyAssembleToMemberId) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Party party = saveParty(ownerEmail);

        Project project = new Project(
                owner, partyAssembleToMemberId, party.getId(), "미게시 프로젝트", PositionType.BACK, LocalDate.now()
        );
        project.complete(LocalDate.now());

        return goalRepository.save(project).getId();
    }

    @Test
    @DisplayName("전시 성취 목록: 로그인하지 않아도 조회할 수 있다")
    void getShowcaseGoalsWithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("전시 성취 목록: 완료된 자기신고 성취가 목록에 나온다")
    void getShowcaseGoalsIncludesAchievedSelfReported() throws Exception {
        long goalId = saveAchievedChecklist("user1@test.com", "자격증 취득");

        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + goalId + ")]").exists());
    }

    @Test
    @DisplayName("전시 성취 목록: 진행 중인 성취는 목록에서 제외된다")
    void getShowcaseGoalsExcludesInProgress() throws Exception {
        long goalId = saveInProgressChecklist("user1@test.com");

        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + goalId + ")]").doesNotExist());
    }

    @Test
    @DisplayName("전시 성취 목록: PROJECT는 전시글을 게시해야 목록에 나온다")
    void getShowcaseGoalsExcludesUnpublishedProject() throws Exception {
        long goalId = saveUnpublishedProject("user1@test.com", 301L);

        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + goalId + ")]").doesNotExist());
    }

    @Test
    @DisplayName("전시 성취 목록: PROJECT는 전시글이 게시되면 party 정보와 함께 목록에 나온다")
    void getShowcaseGoalsIncludesPublishedProjectWithParty() throws Exception {
        long goalId = savePublishedProject("user1@test.com", "정산 자동화 API", 302L);

        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + goalId + ")].detail.title")
                        .value(org.hamcrest.Matchers.hasItem("정산 자동화 API")))
                .andExpect(jsonPath("$.data.content[?(@.id == " + goalId + ")].party.name")
                        .value(org.hamcrest.Matchers.hasItem("오락실 팀")));
    }

    @Test
    @DisplayName("전시 성취 목록: type 필터로 CONTEST만 조회할 수 있다")
    void getShowcaseGoalsFilterByType() throws Exception {
        long contestId = saveAchievedContest("user1@test.com", "오락실 공모전 대상");
        long checklistId = saveAchievedChecklist("user1@test.com", "자격증 취득");

        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals")
                .param("type", "CONTEST"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + contestId + ")]").exists())
                .andExpect(jsonPath("$.data.content[?(@.id == " + checklistId + ")]").doesNotExist());
    }

    @Test
    @DisplayName("전시 성취 목록: sort=POPULAR면 좋아요 수 내림차순으로 정렬된다")
    void getShowcaseGoalsSortedByPopular() throws Exception {
        long lowLikeGoalId = saveAchievedChecklist("user1@test.com", "좋아요 적음");
        long highLikeGoalId = saveAchievedChecklist("user1@test.com", "좋아요 많음");

        goalRepository.increaseLikeCount(highLikeGoalId);
        goalRepository.increaseLikeCount(highLikeGoalId);
        goalRepository.increaseLikeCount(lowLikeGoalId);

        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals")
                .param("sort", "POPULAR"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(highLikeGoalId));
    }

    @Test
    @DisplayName("전시 성취 목록: sort 값이 잘못되면 400-1이다")
    void getShowcaseGoalsInvalidSort() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals")
                .param("sort", "INVALID"));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("전시 성취 목록: 페이징이 동작한다")
    void getShowcaseGoalsPaging() throws Exception {
        saveAchievedChecklist("user1@test.com", "성취1");
        saveAchievedChecklist("user1@test.com", "성취2");
        saveAchievedChecklist("user1@test.com", "성취3");

        ResultActions resultActions = mvc.perform(get("/api/v1/showcase/goals")
                .param("page", "0")
                .param("size", "2"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }
}
