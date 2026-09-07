package com.back.domain.party.application.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestContainerConfig.class)
public class ApiV1ReceivedApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private GoalRepository goalRepository;

    /** user1 이 파티장인 파티에 user2(BACK)·user3(FRONT)가 지원한 상태를 만든다. */
    private Party setUpMyParty() {
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member backApplicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        Member frontApplicant = memberRepository.findByEmail("user3@test.com").orElseThrow();

        Party party = partyRepository.save(newParty(owner));

        // user2 는 프로필과 성취가 있다 - 자기신고 2건, 자동기록(PROJECT) 1건
        memberProfileRepository.save(new MemberProfile(
                backApplicant, "백엔드유저", null, PositionType.BACK, List.of("Java", "Spring")));
        goalRepository.save(new PersonalContest(
                backApplicant, GoalStatus.ACHIEVED, "웹 프로젝트 완료", false, "대상",
                LocalDate.now(), null));
        goalRepository.save(new PersonalChecklist(
                backApplicant, GoalStatus.IN_PROGRESS, "정보처리기사", null, null));
        goalRepository.save(new Project(
                backApplicant, null, party.getId(),
                "커머스 클론", PositionType.BACK, LocalDate.now()));
        partyMemberRepository.save(new PartyMember(
                party, backApplicant, findPosition(party, PositionType.BACK), "지원합니다"));

        // user3 는 프로필을 만든 적이 없다
        partyMemberRepository.save(new PartyMember(
                party, frontApplicant, findPosition(party, PositionType.FRONT), null));

        // 남의 파티에 들어온 지원 - 내 목록에 섞이면 안 된다
        Party othersParty = partyRepository.save(newParty(frontApplicant));
        partyMemberRepository.save(new PartyMember(
                othersParty, backApplicant, findPosition(othersParty, PositionType.BACK), null));

        return party;
    }

    @Test
    @DisplayName("받은 지원자: 내가 파티장인 파티의 지원만, 지원자 프로필·성취와 함께 내려준다")
    @WithUserDetails("user1@test.com")
    void getReceivedApplications() throws Exception {
        Party party = setUpMyParty();

        mvc.perform(get("/api/v1/members/me/received-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("받은 지원자 목록 조회 성공"))
                // 남의 파티 지원 1건은 빠지고 내 파티 2건만 남는다
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.content[0].partyId").value(party.getId()))
                .andExpect(jsonPath("$.data.content[0].partyName").value("파티"));
    }

    @Test
    @DisplayName("받은 지원자: part 로 포지션을 걸러 프로필·성취가 붙은 카드를 확인한다")
    @WithUserDetails("user1@test.com")
    void getReceivedApplicationsFilteredByPart() throws Exception {
        setUpMyParty();
        Member backApplicant = memberRepository.findByEmail("user2@test.com").orElseThrow();

        mvc.perform(get("/api/v1/members/me/received-applications").param("part", "BACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].applicant.id").value(backApplicant.getId()))
                .andExpect(jsonPath("$.data.content[0].applicant.nickname").value("백엔드유저"))
                .andExpect(jsonPath("$.data.content[0].applicant.preferredPosition").value("BACK"))
                .andExpect(jsonPath("$.data.content[0].applicant.techStacks").value(
                        org.hamcrest.Matchers.containsInAnyOrder("Java", "Spring")))
                .andExpect(jsonPath("$.data.content[0].position").value("BACK"))
                .andExpect(jsonPath("$.data.content[0].state").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].message").value("지원합니다"))
                .andExpect(jsonPath("$.data.content[0].achievements.platformVerified").value(1))
                .andExpect(jsonPath("$.data.content[0].achievements.selfReported").value(2))
                .andExpect(jsonPath("$.data.content[0].createDate").exists());
    }

    @Test
    @DisplayName("받은 지원자: 프로필이 없는 지원자는 nickname 이 null 이고 목록이 비어 있다")
    @WithUserDetails("user1@test.com")
    void getReceivedApplicationsWithoutProfile() throws Exception {
        setUpMyParty();

        mvc.perform(get("/api/v1/members/me/received-applications").param("part", "FRONT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].message").isEmpty())
                .andExpect(jsonPath("$.data.content[0].applicant.nickname").isEmpty())
                .andExpect(jsonPath("$.data.content[0].applicant.preferredPosition").isEmpty())
                .andExpect(jsonPath("$.data.content[0].applicant.techStacks").isEmpty())
                .andExpect(jsonPath("$.data.content[0].achievements.platformVerified").value(0))
                .andExpect(jsonPath("$.data.content[0].achievements.selfReported").value(0));
    }

    @Test
    @DisplayName("받은 지원자: partyId 로 특정 파티만 본다")
    @WithUserDetails("user1@test.com")
    void getReceivedApplicationsFilteredByParty() throws Exception {
        Party party = setUpMyParty();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member applicant = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // 내가 파티장인 다른 파티 - partyId 를 주면 여기 지원은 빠져야 한다
        Party another = partyRepository.save(newParty(owner));
        partyMemberRepository.save(new PartyMember(
                another, applicant, findPosition(another, PositionType.BACK), null));

        mvc.perform(get("/api/v1/members/me/received-applications"))
                .andExpect(jsonPath("$.data.content.length()").value(3));

        mvc.perform(get("/api/v1/members/me/received-applications")
                        .param("partyId", String.valueOf(party.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("받은 지원자: 파티가 하나도 없으면 빈 목록이다 - 지원자 id 가 비어도 조회가 터지지 않는다")
    @WithUserDetails("user1@test.com")
    void getReceivedApplicationsWhenEmpty() throws Exception {
        mvc.perform(get("/api/v1/members/me/received-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("받은 지원자: 로그인하지 않으면 401-1이다")
    void getReceivedApplicationsWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/members/me/received-applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("받은 지원자: 남의 파티는 partyId 로 찍어도 볼 수 없다")
    @WithUserDetails("user1@test.com")
    void getReceivedApplicationsCannotPeekOthersParty() throws Exception {
        Member stranger = memberRepository.findByEmail("user3@test.com").orElseThrow();
        Member applicant = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // user3 이 파티장인 파티. user1 이 그 partyId 를 알아내 찍어도 지원자가 보이면 안 된다.
        Party othersParty = partyRepository.save(newParty(stranger));
        partyMemberRepository.save(new PartyMember(
                othersParty, applicant, findPosition(othersParty, PositionType.BACK), "남의 파티 지원"));

        mvc.perform(get("/api/v1/members/me/received-applications")
                        .param("partyId", String.valueOf(othersParty.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("받은 지원자: part 가 정의된 포지션이 아니면 400-1")
    @WithUserDetails("user1@test.com")
    void getReceivedApplicationsWithInvalidPart() throws Exception {
        mvc.perform(get("/api/v1/members/me/received-applications").param("part", "DESIGNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    private Party newParty(Member owner) {
        Party party = new Party(
                owner, "파티", "제목", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null, 1,
                LocalDateTime.now().plusDays(7));
        party.addPosition(new Position(PositionType.BACK, 2));
        party.addPosition(new Position(PositionType.FRONT, 2));

        return party;
    }

    private Position findPosition(Party party, PositionType type) {
        return party.getPositions().stream()
                .filter(position -> position.getType() == type)
                .findFirst()
                .orElseThrow();
    }
}
