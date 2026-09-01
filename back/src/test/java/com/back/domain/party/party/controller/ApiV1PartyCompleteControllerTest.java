package com.back.domain.party.party.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
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
public class ApiV1PartyCompleteControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

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

    private Party savePartyInProgress(String ownerEmail) throws Exception {
        Party party = saveParty(ownerEmail);
        mvc.perform(post("/api/v1/parties/" + party.getId() + "/close-recruiting"))
                .andExpect(status().isCreated());
        return party;
    }

    // API 호출자가 파티장이어야 하는 제약 때문에, 로그인 사용자와 파티 소유자가 다른 테스트(403 케이스)에서는 API를 거치지 않고 엔티티 상태를 직접 세팅한다.
    private Party savePartyInProgressDirectly(String ownerEmail) {
        Party party = saveParty(ownerEmail);
        party.closeRecruiting();
        return partyRepository.save(party);
    }

    @Test
    @DisplayName("파티 완료: 200-1과 함께 COMPLETED로 상태가 전환된다")
    @WithUserDetails("user1@test.com")
    void complete() throws Exception {
        Party party = savePartyInProgress("user1@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/complete"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        Party updated = partyRepository.findById(party.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PartyStatus.COMPLETED);
    }

    @Test
    @DisplayName("파티 완료: 모집 마감 없이(RECRUITING 상태에서) 바로 완료 시도하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void completeFromRecruiting() throws Exception {
        Party party = saveParty("user1@test.com"); // close-recruiting 안 거침 - 여전히 RECRUITING

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/complete"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("파티 완료: 이미 완료된 파티에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void completeTwice() throws Exception {
        Party party = savePartyInProgress("user1@test.com");

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/complete"))
                .andExpect(status().isOk());

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/complete"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("파티 완료: 파티장이 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void completeAsNonOwner() throws Exception {
        Party party = savePartyInProgressDirectly("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/complete"));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("파티 완료: 존재하지 않는 파티면 404-1이다")
    @WithUserDetails("user1@test.com")
    void completeNonExistentParty() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/parties/999999/complete"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
