package com.back.domain.party.partyPr.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.service.PartyService;
import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.model.GithubPullRequestSnapshot;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.domain.party.partyPr.service.PartyPrSseService;
import com.back.domain.party.party.repository.PartyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PartyPrSecurityControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private PartyRepository partyRepository;
    @Autowired private PartyService partyService;
    @Autowired private PartyMemberRepository partyMemberRepository;
    @Autowired private PartyPrRepository partyPrRepository;
    @Autowired private PartyPrSseService partyPrSseService;

    private Party saveParty(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();
        if (memberProfileRepository.findByMember(owner).isEmpty()) {
            memberProfileRepository.save(new MemberProfile(owner, null, null, PositionType.BACK, List.of()));
        }

        long partyId = partyService.create(
                owner, "PR 팀", "PR 테스트", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 2))
        ).id();
        Party saved = partyRepository.findById(partyId).orElseThrow();
        partyPrRepository.save(new PartyPr(saved, new GithubPullRequestSnapshot(
                1001L, 1, "보안 PR", "https://github.com/org/repo/pull/1", "open", 12345L,
                "author", false, false, "main", "feature", OffsetDateTime.now(), null, null, OffsetDateTime.now())));
        return saved;
    }

    private PartyPr savePullRequest(Party party, long githubPrId, long authorGithubUserId, String author, String title) {
        return partyPrRepository.save(new PartyPr(party, new GithubPullRequestSnapshot(
                githubPrId, Math.toIntExact(githubPrId), title,
                "https://github.com/org/repo/pull/" + githubPrId, "open", authorGithubUserId,
                author, false, false, "main", "feature/" + author, OffsetDateTime.now(), null, null,
                OffsetDateTime.now())));
    }

    private PartyMember approve(Party party, Member member) {
        PartyMember partyMember = new PartyMember(party, member, party.getPositions().getFirst(), "참여");
        partyMember.approve();
        return partyMemberRepository.save(partyMember);
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
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests/grouped-by-member"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultCode").value("403-1"));
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests/members/me"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultCode").value("403-1"));
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests/members/" + party.getOwner().getId()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultCode").value("403-1"));
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests/grouped-by-member/stream"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests/members/me/stream"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/parties/" + party.getId() + "/pull-requests/members/" + party.getOwner().getId() + "/stream"))
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

    @Test
    @DisplayName("담당자별 PR 조회는 파티장과 승인 멤버를 GitHub user id로 묶는다")
    @WithUserDetails("user1@test.com")
    void groupsPullRequestsByOwnerAndApprovedMember() throws Exception {
        Party party = saveParty("user2@test.com");
        Member owner = party.getOwner();
        Member approvedMember = memberRepository.findByEmail("user1@test.com").orElseThrow();
        owner.linkGithubAppUserId(11001L);
        approvedMember.linkGithubAppUserId(11002L);
        approve(party, approvedMember);
        savePullRequest(party, 2001L, 11001L, "owner-login", "파티장 PR");
        savePullRequest(party, 2002L, 11002L, "member-login", "승인 멤버 PR");

        mvc.perform(get("/api/v1/parties/{partyId}/pull-requests/grouped-by-member", party.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(owner.getId()))
                .andExpect(jsonPath("$.data[0].owner").value(true))
                .andExpect(jsonPath("$.data[0].githubLogin").value("owner-login"))
                .andExpect(jsonPath("$.data[0].pullRequests[0].title").value("파티장 PR"))
                .andExpect(jsonPath("$.data[1].memberId").value(approvedMember.getId()))
                .andExpect(jsonPath("$.data[1].owner").value(false))
                .andExpect(jsonPath("$.data[1].githubLogin").value("member-login"))
                .andExpect(jsonPath("$.data[1].pullRequests[0].title").value("승인 멤버 PR"));
    }

    @Test
    @DisplayName("파티 내 me와 memberId 조회는 선택한 멤버의 PR만 반환한다")
    @WithUserDetails("user1@test.com")
    void readsMineAndAnotherMembersPullRequests() throws Exception {
        Party party = saveParty("user2@test.com");
        Member owner = party.getOwner();
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        owner.linkGithubAppUserId(12001L);
        actor.linkGithubAppUserId(12002L);
        approve(party, actor);
        savePullRequest(party, 3001L, 12001L, "owner-login", "다른 사람 PR");
        savePullRequest(party, 3002L, 12002L, "actor-login", "내 PR");

        mvc.perform(get("/api/v1/parties/{partyId}/pull-requests/members/me", party.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(actor.getId()))
                .andExpect(jsonPath("$.data.pullRequests.length()").value(1))
                .andExpect(jsonPath("$.data.pullRequests[0].title").value("내 PR"));

        mvc.perform(get("/api/v1/parties/{partyId}/pull-requests/members/{memberId}",
                        party.getId(), owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(owner.getId()))
                .andExpect(jsonPath("$.data.pullRequests.length()").value(1))
                .andExpect(jsonPath("$.data.pullRequests[0].title").value("다른 사람 PR"));
    }

    @Test
    @DisplayName("멤버 PR SSE는 선택한 멤버의 변경 이벤트만 전달한다")
    @WithUserDetails("user1@test.com")
    void memberStreamFiltersOtherMembersEvents() throws Exception {
        Party party = saveParty("user2@test.com");
        Member owner = party.getOwner();
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        owner.linkGithubAppUserId(13001L);
        actor.linkGithubAppUserId(13002L);
        approve(party, actor);

        MvcResult stream = mvc.perform(get(
                        "/api/v1/parties/{partyId}/pull-requests/members/me/stream", party.getId()))
                .andExpect(request().asyncStarted())
                .andReturn();

        PartyPr mine = savePullRequest(party, 4001L, 13002L, "actor-login", "SSE 내 PR");
        PartyPr others = savePullRequest(party, 4002L, 13001L, "owner-login", "SSE 다른 사람 PR");
        partyPrSseService.publish(party.getId(), new PartyPrDto(mine));
        partyPrSseService.publish(party.getId(), new PartyPrDto(others));
        partyPrSseService.completeParty(party.getId());

        mvc.perform(asyncDispatch(stream))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"githubPrId\":4001")))
                .andExpect(content().string(not(containsString("\"githubPrId\":4002"))));
    }

    @Test
    @DisplayName("담당자별 PR SSE는 담당자 그룹 snapshot을 반환한다")
    @WithUserDetails("user1@test.com")
    void groupedStreamReturnsGroupedSnapshot() throws Exception {
        Party party = saveParty("user2@test.com");
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        party.getOwner().linkGithubAppUserId(14001L);
        actor.linkGithubAppUserId(14002L);
        approve(party, actor);
        savePullRequest(party, 5001L, 14001L, "owner-login", "그룹 SSE 파티장 PR");
        savePullRequest(party, 5002L, 14002L, "actor-login", "그룹 SSE 멤버 PR");

        MvcResult stream = mvc.perform(get(
                        "/api/v1/parties/{partyId}/pull-requests/grouped-by-member/stream", party.getId()))
                .andExpect(request().asyncStarted())
                .andReturn();
        partyPrSseService.completeParty(party.getId());

        mvc.perform(asyncDispatch(stream))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"githubUserId\":14001")))
                .andExpect(content().string(containsString("\"githubUserId\":14002")));
    }

    @Test
    @DisplayName("담당자별 PR SSE의 증분 이벤트도 그룹 DTO로 전달한다")
    @WithUserDetails("user1@test.com")
    void groupedStreamPublishesGroupedEvent() throws Exception {
        Party party = saveParty("user2@test.com");
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        actor.linkGithubAppUserId(15002L);
        approve(party, actor);

        MvcResult stream = mvc.perform(get(
                        "/api/v1/parties/{partyId}/pull-requests/grouped-by-member/stream", party.getId()))
                .andExpect(request().asyncStarted())
                .andReturn();
        PartyPr pullRequest = savePullRequest(party, 6001L, 15002L, "actor-login", "그룹 SSE 증분 PR");
        partyPrSseService.publishGrouped(party.getId(), new com.back.domain.party.partyPr.dtos.PartyPrByMemberDto(
                actor.getId(), actor.getName(), 15002L, "actor-login", false, List.of(new PartyPrDto(pullRequest))));
        partyPrSseService.completeParty(party.getId());

        mvc.perform(asyncDispatch(stream))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:pull-request-group")))
                .andExpect(content().string(containsString("\"memberId\":" + actor.getId())))
                .andExpect(content().string(containsString("\"githubLogin\":\"actor-login\"")));
    }

    @Test
    @DisplayName("GitHub 미연동 멤버는 빈 그룹으로, 외부 작성자는 별도 그룹으로 반환한다")
    @WithUserDetails("user1@test.com")
    void returnsUnlinkedMemberAndExternalAuthorGroups() throws Exception {
        Party party = saveParty("user2@test.com");
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        approve(party, actor);
        savePullRequest(party, 7001L, 17001L, "external-login", "외부 PR");

        mvc.perform(get("/api/v1/parties/{partyId}/pull-requests/grouped-by-member", party.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value(party.getOwner().getId()))
                .andExpect(jsonPath("$.data[0].githubLogin").doesNotExist())
                .andExpect(content().string(containsString("\"githubLogin\":\"external-login\"")));
    }

    @Test
    @DisplayName("GitHub 작성자 ID가 없는 PR은 unknown external 그룹으로 보존한다")
    @WithUserDetails("user1@test.com")
    void preservesUnknownAuthorGroup() throws Exception {
        Party party = saveParty("user2@test.com");
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        approve(party, actor);
        partyPrRepository.save(new PartyPr(party, new GithubPullRequestSnapshot(
                8001L, 8001, "작성자 미상 PR", "https://github.com/org/repo/pull/8001", "open", null,
                null, false, false, "main", "feature", OffsetDateTime.now(), null, null, OffsetDateTime.now())));

        mvc.perform(get("/api/v1/parties/{partyId}/pull-requests/grouped-by-member", party.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.memberId == null && @.githubUserId == null)].pullRequests[?(@.title == '작성자 미상 PR')]")
                        .exists());
    }

    @Test
    @DisplayName("승인되지 않은 멤버 ID의 PR 조회는 404-2를 반환한다")
    @WithUserDetails("user1@test.com")
    void rejectsUnapprovedMemberId() throws Exception {
        Party party = saveParty("user2@test.com");
        Member actor = memberRepository.findByEmail("user1@test.com").orElseThrow();
        approve(party, actor);

        mvc.perform(get("/api/v1/parties/{partyId}/pull-requests/members/{memberId}", party.getId(), 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"));
    }
}
