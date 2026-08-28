package com.back.domain.interaction.like.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

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
    @DisplayName("파티 좋아요 취소: 204-1과 liked=false, likeCount 감소를 반환한다")
    @WithUserDetails("user1@test.com")
    void unlikeParty() throws Exception {
        Party party = saveParty("user2@test.com");

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + party.getId() + "/likes"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));
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
}
