package com.back.domain.party.party.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.assemble.repository.PartyAssembleRepository;
import com.back.domain.party.assemble.repository.PartyAssembleToMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.position.entity.Position;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1PartyCloseRecruitingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private PartyAssembleRepository partyAssembleRepository;

    @Autowired
    private PartyAssembleToMemberRepository partyAssembleToMemberRepository;

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
        party.addPosition(new Position(PositionType.BACK, 3));

        return partyRepository.save(party);
    }

    @Test
    @DisplayName("모집 마감: 201-1과 함께 IN_PROGRESS로 상태가 전환된다")
    @WithUserDetails("user1@test.com")
    void closeRecruiting() throws Exception {
        Party party = saveParty("user1@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        Party updated = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PartyStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("모집 마감: PENDING 지원 건은 REJECTED로, APPROVED는 그대로 유지된다")
    @WithUserDetails("user1@test.com")
    void closeRecruitingRejectsPendingApplications() throws Exception {
        Party party = saveParty("user1@test.com");
        Position position = party.getPositions().get(0);

        Member pendingApplicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        PartyMember pendingMember = partyMemberRepository.save(
                new PartyMember(party, pendingApplicant, position, null));

        Member approvedApplicant = memberRepository.findByEmail("user3@test.com").orElseThrow();
        PartyMember approvedMember = new PartyMember(party, approvedApplicant, position, null);
        approvedMember.approve();
        partyMemberRepository.save(approvedMember);

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"))
                .andExpect(status().isCreated());

        PartyMember updatedPending = partyMemberRepository.findByIdAndParty(pendingMember.getId(), party).orElseThrow();
        PartyMember updatedApproved = partyMemberRepository.findByIdAndParty(approvedMember.getId(), party).orElseThrow();

        assertThat(updatedPending.getStatus()).isEqualTo(PartyMemberStatus.REJECTED);
        assertThat(updatedApproved.getStatus()).isEqualTo(PartyMemberStatus.APPROVED);
    }

    @Test
    @DisplayName("모집 마감: APPROVED 인원만큼 PartyAssemble/PartyAssembleToMember가 기록된다")
    @WithUserDetails("user1@test.com")
    void closeRecruitingRecordsAssemble() throws Exception {
        Party party = saveParty("user1@test.com");
        Position position = party.getPositions().get(0);

        Member approvedApplicant = memberRepository.findByEmail("user2@test.com").orElseThrow();
        PartyMember approvedMember = new PartyMember(party, approvedApplicant, position, null);
        approvedMember.approve();
        partyMemberRepository.save(approvedMember);

        long assembleCountBefore = partyAssembleRepository.count();
        long assembleToMemberCountBefore = partyAssembleToMemberRepository.count();

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"))
                .andExpect(status().isCreated());

        assertThat(partyAssembleRepository.count()).isEqualTo(assembleCountBefore + 1);
        assertThat(partyAssembleToMemberRepository.count()).isEqualTo(assembleToMemberCountBefore + 1);
    }

    @Test
    @DisplayName("모집 마감: 이미 마감된 파티에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void closeRecruitingTwice() throws Exception {
        Party party = saveParty("user1@test.com");

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("모집 마감: 파티장이 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void closeRecruitingAsNonOwner() throws Exception {
        Party party = saveParty("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("모집 마감: 존재하지 않는 파티면 404-1이다")
    @WithUserDetails("user1@test.com")
    void closeRecruitingNonExistentParty() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/parties/999999/close-recruiting"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
