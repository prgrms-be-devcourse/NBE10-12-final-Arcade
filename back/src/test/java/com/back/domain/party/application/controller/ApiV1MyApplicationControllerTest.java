package com.back.domain.party.application.controller;

import com.back.RedisTestContainerConfig;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.assemble.entity.PartyAssemble;
import com.back.domain.party.assemble.entity.PartyAssembleToMember;
import com.back.domain.party.assemble.repository.PartyAssembleRepository;
import com.back.domain.party.assemble.repository.PartyAssembleToMemberRepository;
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
    private PartyAssembleRepository partyAssembleRepository;

    @Autowired
    private PartyAssembleToMemberRepository partyAssembleToMemberRepository;

    @Test
    @DisplayName("내 지원 현황: 대기 중인 지원만 나온다 - 승인·거절된 건은 빠진다")
    @WithUserDetails("user1@test.com")
    void getMyApplications() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        Party first = partyRepository.save(newParty(owner, "먼저 지원한 파티"));
        partyMemberRepository.save(new PartyMember(
                first, actor, first.getPositions().getFirst(), "지원합니다"));

        Party second = partyRepository.save(newParty(owner, "나중에 지원한 파티"));
        partyMemberRepository.save(new PartyMember(
                second, actor, second.getPositions().getFirst(), null));

        // 거절된 지원 - 목록에 나오면 안 된다
        Party rejectedParty = partyRepository.save(newParty(owner, "거절된 파티"));
        PartyMember rejected = new PartyMember(
                rejectedParty, actor, rejectedParty.getPositions().getFirst(), null);
        rejected.reject();
        partyMemberRepository.save(rejected);

        // 승인돼서 이미 내 파티가 된 건 - 이것도 나오면 안 된다
        Party approvedParty = partyRepository.save(newParty(owner, "승인된 파티"));
        PartyMember approved = new PartyMember(
                approvedParty, actor, approvedParty.getPositions().getFirst(), null);
        approved.approve();
        partyMemberRepository.save(approved);

        mvc.perform(get("/api/v1/members/me/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 지원 현황 조회 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                // 최신순이라 나중에 지원한 쪽이 먼저다
                .andExpect(jsonPath("$.data.content[0].party.id").value(second.getId()))
                .andExpect(jsonPath("$.data.content[0].party.name").value("나중에 지원한 파티"))
                .andExpect(jsonPath("$.data.content[1].party.id").value(first.getId()))
                .andExpect(jsonPath("$.data.content[1].party.name").value("먼저 지원한 파티"))
                .andExpect(jsonPath("$.data.content[1].position").value("BACK"))
                .andExpect(jsonPath("$.data.content[1].state").value("PENDING"));
    }

    @Test
    @DisplayName("내 지원 현황: 다음 페이지가 남아 있으면 hasNext 가 true 다")
    @WithUserDetails("user1@test.com")
    void getMyApplicationsHasNext() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        for (int i = 0; i < 2; i++) {
            Party party = partyRepository.save(newParty(owner, "파티"));
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

        // 같은 파티에 나는 BACK, 남은 FRONT 로 지원한다.
        // 남의 행을 집어오면 position 이 FRONT 로 나와서 걸린다.
        Party party = partyRepository.save(newParty(owner, "파티"));
        partyMemberRepository.save(new PartyMember(
                party, actor, findPosition(party, PositionType.BACK), "내 지원"));
        partyMemberRepository.save(new PartyMember(
                party, stranger, findPosition(party, PositionType.FRONT), "남의 지원"));

        mvc.perform(get("/api/v1/members/me/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].party.id").value(party.getId()))
                .andExpect(jsonPath("$.data.content[0].position").value("BACK"))
                .andExpect(jsonPath("$.data.content[0].state").value("PENDING"));
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

    @Test
    @DisplayName("참여 파티 히스토리: 확정 명단에 든 파티만 나온다 - 모집 중이면 승인받았어도 빠진다")
    @WithUserDetails("user1@test.com")
    void getMyParties() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // 확정 명단에 든 파티 - 이것만 나와야 한다
        Party assembled = partyRepository.save(newParty(owner, "확정된 파티"));
        PartyMember approved = new PartyMember(
                assembled, actor, findPosition(assembled, PositionType.BACK), null);
        approved.approve();
        partyMemberRepository.save(approved);
        assembled.closeRecruiting();
        PartyAssemble partyAssemble = partyAssembleRepository.save(new PartyAssemble(assembled));
        partyAssembleToMemberRepository.save(new PartyAssembleToMember(partyAssemble, actor));

        // 승인은 받았지만 파티장이 아직 모집을 안 닫은 파티 - 관리 탭 소관이라 여기 나오면 안 된다
        Party stillRecruiting = partyRepository.save(newParty(owner, "아직 모집 중인 파티"));
        PartyMember approvedButRecruiting = new PartyMember(
                stillRecruiting, actor, findPosition(stillRecruiting, PositionType.BACK), null);
        approvedButRecruiting.approve();
        partyMemberRepository.save(approvedButRecruiting);

        // 내가 열었지만 아직 모집 중인 파티 - 확정 전이라 마찬가지로 빠진다
        partyRepository.save(newParty(actor, "내가 연 모집 중 파티"));

        // 승인 대기 / 거절 - 애초에 참여가 아니다
        Party pendingParty = partyRepository.save(newParty(owner, "지원만 한 파티"));
        partyMemberRepository.save(new PartyMember(
                pendingParty, actor, pendingParty.getPositions().getFirst(), null));

        mvc.perform(get("/api/v1/members/me/parties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].partyName").value("확정된 파티"))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[0].positionType").value("BACK"))
                // 기획서 2.11 이 요구한 주제 유형
                .andExpect(jsonPath("$.data[0].topicType").value("PROJECT"))
                .andExpect(jsonPath("$.data[0].exhibited").value(false));
    }

    @Test
    @DisplayName("참여 파티 히스토리: 내가 연 파티도 확정되면 파티장으로 나온다")
    @WithUserDetails("user1@test.com")
    void getMyPartiesIncludesOwnedParty() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();

        // 확정 때 파티장도 명단 맨 앞에 들어간다(PartyLifecycleService.closeRecruiting)
        Party myParty = partyRepository.save(newParty(actor, "내가 연 파티"));
        myParty.closeRecruiting();
        PartyAssemble partyAssemble = partyAssembleRepository.save(new PartyAssemble(myParty));
        partyAssembleToMemberRepository.save(new PartyAssembleToMember(partyAssemble, actor));

        mvc.perform(get("/api/v1/members/me/parties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                // 파티장은 지원 절차가 없어 포지션이 없다
                .andExpect(jsonPath("$.data[0].positionType")
                        .value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("참여 파티 히스토리: 확정 명단에서 빠지면 승인받았어도 안 나온다")
    @WithUserDetails("user1@test.com")
    void getMyPartiesFollowsAssembleRoster() throws Exception {
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        // 승인은 받았지만 파티장이 확정 명단에서 뺀 파티.
        // 요약의 completedParties 건수도 명단으로 세므로, 여기가 승인 기준이면 숫자와 목록이 어긋난다.
        Party dropped = partyRepository.save(newParty(owner, "명단에서 빠진 파티"));
        PartyMember approvedButDropped = new PartyMember(
                dropped, actor, findPosition(dropped, PositionType.BACK), null);
        approvedButDropped.approve();
        partyMemberRepository.save(approvedButDropped);
        dropped.closeRecruiting();
        PartyAssemble assemble = partyAssembleRepository.save(new PartyAssemble(dropped));
        partyAssembleToMemberRepository.save(new PartyAssembleToMember(assemble, owner));

        mvc.perform(get("/api/v1/members/me/parties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("참여 파티 히스토리: 미로그인이면 401")
    void getMyPartiesWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/members/me/parties"))
                .andExpect(status().isUnauthorized());
    }

    private Party newParty(Member owner, String partyName) {
        Party party = new Party(
                owner, partyName, "제목", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null,
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
