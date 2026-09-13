package com.back.domain.party.recommendation.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.recommendation.entity.PartyRecommendation;
import com.back.domain.party.recommendation.repository.PartyRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PartyRecommendationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyRecommendationRepository partyRecommendationRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.findByEmail("user1@test.com").orElseThrow();
        memberProfileRepository.findByMember(member).ifPresentOrElse(
                p -> {},
                () -> memberProfileRepository.save(new MemberProfile(member, null, null, PositionType.BACK, List.of()))
        );
    }

    @Test
    @DisplayName("추천 파티 조회: 로그인한 회원이 요청하면 추천 목록을 반환한다")
    @WithUserDetails("user1@test.com")
    void recommendations_byLoggedInMember_returns200() throws Exception {
        Party party = partyRepository.save(new Party(
                member, "추천용 파티", "추천용 파티 제목", "설명", null, null, null,
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)
        ));
        partyRecommendationRepository.save(new PartyRecommendation(member.getId(), party.getId(), 1, "관심 기술 스택 일치"));

        mvc.perform(get("/api/v1/parties/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.profileRequired").value(false))
                .andExpect(jsonPath("$.data.items[0].party.title").value("추천용 파티 제목"))
                .andExpect(jsonPath("$.data.items[0].reason").value("관심 기술 스택 일치"));
    }

    @Test
    @DisplayName("추천 파티 조회: 로그인하지 않았으면 401-1이다")
    void recommendations_withoutLogin_returns401() throws Exception {
        mvc.perform(get("/api/v1/parties/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
