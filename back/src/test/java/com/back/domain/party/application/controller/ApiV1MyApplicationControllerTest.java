package com.back.domain.party.application.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RedisTestContainerConfig.class)
public class ApiV1MyApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyMemberRepository partyMemberRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Test
    @DisplayName("내 지원 현황: 대기 중인 지원만 최신순으로, 파티·포지션과 함께 내려준다")
    @WithUserDetails("user1@test.com")
    void getMyApplications() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        Contest contest = contestRepository.save(new Contest(
                owner.getId(), "AI 해커톤", ContestFormat.HACKATHON, ContestTag.AI,
                LocalDate.now(), LocalDate.now().plusDays(30)));

        // 등록 대회와 연결된 파티 - contestTag 가 채워져야 한다
        Party linked = partyRepository.save(newParty(owner, contest));
        partyMemberRepository.save(new PartyMember(
                linked, actor, linked.getPositions().getFirst(), "지원합니다"));

        // 대회 연결이 없는 파티 - contestTag 는 null 이어야 한다
        Party unlinked = partyRepository.save(newParty(owner, null));
        partyMemberRepository.save(new PartyMember(
                unlinked, actor, unlinked.getPositions().getFirst(), null));

        // 거절된 지원 - 목록에 나오면 안 된다
        Party rejectedParty = partyRepository.save(newParty(owner, null));
        PartyMember rejected = new PartyMember(
                rejectedParty, actor, rejectedParty.getPositions().getFirst(), null);
        rejected.reject();
        partyMemberRepository.save(rejected);

        mvc.perform(get("/api/v1/members/me/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 지원 현황 조회 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                // 최신순이라 나중에 지원한 unlinked 가 먼저다
                .andExpect(jsonPath("$.data.content[0].party.id").value(unlinked.getId()))
                .andExpect(jsonPath("$.data.content[0].party.contestTag").isEmpty())
                .andExpect(jsonPath("$.data.content[1].party.id").value(linked.getId()))
                .andExpect(jsonPath("$.data.content[1].party.name").value("파티"))
                .andExpect(jsonPath("$.data.content[1].party.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.data.content[1].party.topicType").value("CONTEST"))
                .andExpect(jsonPath("$.data.content[1].party.contestTag").value("AI"))
                .andExpect(jsonPath("$.data.content[1].party.status").value("RECRUITING"))
                .andExpect(jsonPath("$.data.content[1].position.name").value("BACK"))
                .andExpect(jsonPath("$.data.content[1].position.capacity").value(2))
                .andExpect(jsonPath("$.data.content[1].state").value("PENDING"))
                .andExpect(jsonPath("$.data.content[1].createDate").exists());
    }

    @Test
    @DisplayName("내 지원 현황: 다음 페이지가 남아 있으면 hasNext 가 true 다")
    @WithUserDetails("user1@test.com")
    void getMyApplicationsHasNext() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        for (int i = 0; i < 2; i++) {
            Party party = partyRepository.save(newParty(owner, null));
            partyMemberRepository.save(new PartyMember(
                    party, actor, party.getPositions().getFirst(), null));
        }

        mvc.perform(get("/api/v1/members/me/applications").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mvc.perform(get("/api/v1/members/me/applications")
                        .param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("내 지원 현황: 남의 지원은 섞이지 않는다 - 같은 파티에 함께 지원했어도 내 것만 나온다")
    @WithUserDetails("user1@test.com")
    void getMyApplicationsExcludesOthers() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();
        Member stranger = memberRepository.findByEmail("user3@test.com").orElseThrow();

        Party party = partyRepository.save(newParty(owner, null));
        partyMemberRepository.save(new PartyMember(
                party, actor, party.getPositions().getFirst(), "내 지원"));
        partyMemberRepository.save(new PartyMember(
                party, stranger, party.getPositions().getFirst(), "남의 지원"));

        mvc.perform(get("/api/v1/members/me/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(
                        partyMemberRepository.findByPartyAndMember(party, actor).orElseThrow().getId()));
    }

    @Test
    @DisplayName("내 지원 현황: 로그인하지 않으면 401-1이다")
    void getMyApplicationsWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/members/me/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("내 지원 현황: size 가 범위를 벗어나면 400-1")
    @WithUserDetails("user1@test.com")
    void getMyApplicationsWithInvalidSize() throws Exception {
        mvc.perform(get("/api/v1/members/me/applications").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    private Party newParty(Member owner, Contest contest) {
        Party party = new Party(
                owner, "파티", "제목", "설명", contest, null, null,
                TopicType.CONTEST, PartyTag.WEB, null, 1,
                LocalDateTime.now().plusDays(7));
        party.addPosition(new Position(PositionType.BACK, 2));

        return party;
    }
}
