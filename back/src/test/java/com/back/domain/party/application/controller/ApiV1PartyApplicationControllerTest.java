package com.back.domain.party.application.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.position.repository.PositionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1PartyApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private PositionRepository positionRepository;

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

    private String applyRequestJson(long positionId, String message) {
        return """
            {
                "positionId": %d,
                "message": %s
            }
            """.formatted(positionId, message == null ? "null" : "\"" + message + "\"");
    }

    @Test
    @DisplayName("파티 지원: 201-1과 지원 정보를 반환한다")
    @WithUserDetails("user1@test.com")
    void apply() throws Exception {
        Party party = saveParty("user2@test.com");
        long positionId = party.getPositions().get(0).getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applyRequestJson(positionId, "열심히 하겠습니다")));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"));
    }

    @Test
    @DisplayName("파티 지원: 본인이 만든 파티에 지원하면 409-2이다")
    @WithUserDetails("user1@test.com")
    void applyToOwnParty() throws Exception {
        Party party = saveParty("user1@test.com");
        long positionId = party.getPositions().get(0).getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applyRequestJson(positionId, null)));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-2"));
    }

    @Test
    @DisplayName("파티 지원: 이미 지원했으면 409-1이다")
    @WithUserDetails("user1@test.com")
    void applyTwice() throws Exception {
        Party party = saveParty("user2@test.com");
        long positionId = party.getPositions().get(0).getId();

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyRequestJson(positionId, null)))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applyRequestJson(positionId, null)));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("파티 지원: 모집이 종료된 파티에 지원하면 409-3이다")
    @WithUserDetails("user1@test.com")
    void applyToClosedParty() throws Exception {
        Party party = saveParty("user2@test.com");
        long positionId = party.getPositions().get(0).getId();

        // close-recruiting API가 아직 없어 테스트에서 직접 상태를 바꿔 세팅
        ReflectionTestUtils.setField(party, "status", PartyStatus.IN_PROGRESS);
        partyRepository.save(party);

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applyRequestJson(positionId, null)));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-3"));
    }

    @Test
    @DisplayName("파티 지원: 지원 메시지가 50자를 초과하면 400-1이다")
    @WithUserDetails("user1@test.com")
    void applyWithTooLongMessage() throws Exception {
        Party party = saveParty("user2@test.com");
        long positionId = party.getPositions().get(0).getId();
        String longMessage = "가".repeat(51);

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applyRequestJson(positionId, longMessage)));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("파티 지원: 존재하지 않는 포지션이면 404-1이다")
    @WithUserDetails("user1@test.com")
    void applyToNonExistentPosition() throws Exception {
        Party party = saveParty("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applyRequestJson(999999L, null)));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("지원자 목록 조회: 파티장이면 200-1과 지원자 목록을 반환한다")
    @WithUserDetails("user1@test.com")
    void listApplicationsAsOwner() throws Exception {
        Party party = saveParty("user1@test.com");
        Position position = party.getPositions().get(0);
        Member applicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        partyMemberRepository.save(new PartyMember(party, applicant, position, "지원합니다"));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/" + party.getId() + "/applications"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].applicantName").isNotEmpty());
    }

    @Test
    @DisplayName("지원자 목록 조회: 파티장이 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void listApplicationsAsNonOwner() throws Exception {
        Party party = saveParty("user2@test.com");

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/" + party.getId() + "/applications"));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    private String decisionRequestJson(String pending) {
        return """
            { "pending": "%s" }
            """.formatted(pending);
    }

    @Test
    @DisplayName("지원 승인: 200-1과 APPROVED 상태를 반환하고 정원이 채워진다")
    @WithUserDetails("user1@test.com")
    void approveApplication() throws Exception {
        Party party = saveParty("user1@test.com");
        Position position = party.getPositions().get(0);
        Member applicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        PartyMember partyMember = partyMemberRepository.save(new PartyMember(party, applicant, position, null));

        ResultActions resultActions = mvc.perform(patch(
                "/api/v1/parties/" + party.getId() + "/applications/" + partyMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequestJson("APPROVED")));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        Position updatedPosition = positionRepository.findById(position.getId()).orElseThrow();
        assertThat(updatedPosition.getFilledCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("지원 거절: 200-1과 REJECTED 상태를 반환한다")
    @WithUserDetails("user1@test.com")
    void rejectApplication() throws Exception {
        Party party = saveParty("user1@test.com");
        Position position = party.getPositions().get(0);
        Member applicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        PartyMember partyMember = partyMemberRepository.save(new PartyMember(party, applicant, position, null));

        ResultActions resultActions = mvc.perform(patch(
                "/api/v1/parties/" + party.getId() + "/applications/" + partyMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequestJson("REJECTED")));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("지원 승인/거절: 이미 처리된 지원건을 재처리하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void decideAlreadyProcessedApplication() throws Exception {
        Party party = saveParty("user1@test.com");
        Position position = party.getPositions().get(0);
        Member applicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        PartyMember partyMember = partyMemberRepository.save(new PartyMember(party, applicant, position, null));

        mvc.perform(patch("/api/v1/parties/" + party.getId() + "/applications/" + partyMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionRequestJson("APPROVED")))
                .andExpect(status().isOk());

        ResultActions resultActions = mvc.perform(patch(
                "/api/v1/parties/" + party.getId() + "/applications/" + partyMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequestJson("REJECTED")));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("지원 승인: 정원이 이미 마감됐으면 409-2이다")
    @WithUserDetails("user1@test.com")
    void approveWhenPositionFull() throws Exception {
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
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
        party.addPosition(new Position(PositionType.BACK, 1)); // 정원 1명
        party = partyRepository.save(party);
        Position position = party.getPositions().get(0);

        // user2가 이미 승인되어 정원(1명)을 다 채운 상태
        Member firstApplicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        PartyMember approvedMember = new PartyMember(party, firstApplicant, position, null);
        approvedMember.approve();
        position.fillOneSeat();
        partyMemberRepository.save(approvedMember);
        positionRepository.save(position);

        // user3이 뒤늦게 지원 -> PENDING
        Member secondApplicant = memberRepository.findByEmail("user3@test.com").orElseThrow();
        PartyMember pendingMember = partyMemberRepository.save(
                new PartyMember(party, secondApplicant, position, null));

        ResultActions resultActions = mvc.perform(patch(
                "/api/v1/parties/" + party.getId() + "/applications/" + pendingMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequestJson("APPROVED")));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-2"));
    }

    @Test
    @DisplayName("지원 승인/거절: 파티장이 아니면 403-1이다")
    @WithUserDetails("user2@test.com")
    void decideAsNonOwner() throws Exception {
        Party party = saveParty("user1@test.com");
        Position position = party.getPositions().get(0);
        Member applicant = memberRepository.findByEmail("user3@test.com").orElseThrow();
        PartyMember partyMember = partyMemberRepository.save(new PartyMember(party, applicant, position, null));

        ResultActions resultActions = mvc.perform(patch(
                "/api/v1/parties/" + party.getId() + "/applications/" + partyMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequestJson("APPROVED")));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("지원 승인/거절: 존재하지 않는 지원 건이면 404-1이다")
    @WithUserDetails("user1@test.com")
    void decideNonExistentApplication() throws Exception {
        Party party = saveParty("user1@test.com");

        ResultActions resultActions = mvc.perform(patch(
                "/api/v1/parties/" + party.getId() + "/applications/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequestJson("APPROVED")));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
