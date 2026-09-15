package com.back.domain.party.party.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.assemble.repository.PartyAssembleRepository;
import com.back.domain.party.assemble.repository.PartyAssembleToMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.event.PartyAssembledEvent;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
public class ApiV1PartyCloseRecruitingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private ApplicationEvents events;

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
        // 승인된 지원자 1명 + 이 시점에 합류하는 파티장
        assertThat(partyAssembleToMemberRepository.count()).isEqualTo(assembleToMemberCountBefore + 2);
    }

    @Test
    @DisplayName("모집 마감: 파티장은 PartyMember 없이 확정 명단에 바로 들어가고, 포지션은 프로필 대표 포지션을 따라간다")
    @WithUserDetails("user1@test.com")
    void closeRecruitingAddsOwnerToAssemble() throws Exception {
        Party party = saveParty("user1@test.com");
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        memberProfileRepository.save(
                new MemberProfile(owner, null, null, PositionType.FRONT, List.of()));

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"))
                .andExpect(status().isCreated());

        assertThat(partyMemberRepository.findByPartyAndMember(party, owner)).isEmpty();
        PartyAssembledEvent event = events.stream(PartyAssembledEvent.class).findFirst().orElseThrow();
        assertThat(event.approvedMembers())
                .anyMatch(m -> m.memberId() == owner.getId() && m.positionType() == PositionType.FRONT);
    }

    @Test
    @DisplayName("모집 마감: 파티장이 옛 방식대로 APPROVED PartyMember 로도 남아 있으면 확정 명단에 한 번만 들어간다")
    @WithUserDetails("user1@test.com")
    void closeRecruitingDoesNotDuplicateLegacyOwnerRow() throws Exception {
        Party party = saveParty("user1@test.com");
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();

        // ARC-97 시절 파티 생성이 만들던 상태 재현 - 파티장이 APPROVED 지원 행으로도 들어가 있다
        PartyMember legacyOwnerRow = partyMemberRepository.save(
                new PartyMember(party, owner, party.getPositions().getFirst(), null));
        legacyOwnerRow.approve();

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"))
                .andExpect(status().isCreated());

        PartyAssembledEvent event = events.stream(PartyAssembledEvent.class).findFirst().orElseThrow();
        assertThat(event.approvedMembers())
                .filteredOn(m -> m.memberId() == owner.getId())
                .hasSize(1);
    }

    @Test
    @DisplayName("모집 마감: 파티장이 프로필에 대표 포지션을 안 넣어뒀으면 포지션 없이(null) 확정 명단에 들어간다")
    @WithUserDetails("user2@test.com")
    void closeRecruitingAllowsOwnerWithoutPosition() throws Exception {
        Party party = saveParty("user2@test.com");
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();
        // 프로필을 만들지 않는다 - 대표 포지션이 없는 상태가 그대로 이 테스트의 조건이다.

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"))
                .andExpect(status().isCreated());

        PartyAssembledEvent event = events.stream(PartyAssembledEvent.class).findFirst().orElseThrow();
        assertThat(event.approvedMembers())
                .anyMatch(m -> m.memberId() == owner.getId() && m.positionType() == null);
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
