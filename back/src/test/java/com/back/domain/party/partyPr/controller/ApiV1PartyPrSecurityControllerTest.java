package com.back.domain.party.partyPr.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.partyPr.model.GithubPullRequestSnapshot;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PartyPrSecurityControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private PartyMemberRepository partyMemberRepository;
    @Autowired private PartyPrRepository partyPrRepository;

    private Party saveParty(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        Party party = new Party(owner, "PR 팀", "PR 테스트", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null, 1, LocalDateTime.now().plusDays(7));
        party.addPosition(new Position(PositionType.BACK, 2));
        Party saved = partyRepository.save(party);
        partyPrRepository.save(new PartyPr(saved, new GithubPullRequestSnapshot(
                1001L, 1, "보안 PR", "https://github.com/org/repo/pull/1", "open", 12345L,
                "author", false, false, "main", "feature", OffsetDateTime.now(), null, null, OffsetDateTime.now())));
        return saved;
    }

    @Test
    @DisplayName("비파티원은 PR 목록, 연결 상태, SSE stream을 조회할 수 없다")
    @WithUserDetails("user1@test.com")
    void nonMemberCannotReadPartyGithubData() throws Exception {
        Party party = saveParty("user2@test.com");

        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultCode").value("403-1"));
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/github-connection"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultCode").value("403-1"));
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests/stream"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("확정 파티원은 PR 목록과 GitHub 연결 상태를 조회할 수 있다")
    @WithUserDetails("user1@test.com")
    void approvedMemberCanReadPartyGithubData() throws Exception {
        Party party = saveParty("user2@test.com");
        Member member = memberRepository.findByEmail("user1@test.com").orElseThrow();
        PartyMember partyMember = new PartyMember(party, member, party.getPositions().getFirst(), "참여");
        partyMember.approve();
        partyMemberRepository.save(partyMember);

        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].authorGithubUserId").value(12345));
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/github-connection"))
                .andExpect(status().isOk());
    }
}
