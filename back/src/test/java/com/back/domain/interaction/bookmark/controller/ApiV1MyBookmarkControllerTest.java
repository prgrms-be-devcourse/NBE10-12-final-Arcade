package com.back.domain.interaction.bookmark.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 북마크함 조회(GET /members/me/bookmarks).
 *
 * 400(page·size 검증)과 401(미로그인) 외에, 500 이 나기 쉬운 자리를 따로 덮는다 -
 * 다형성 저장이라 대상 테이블을 못 찾거나, 전시가 내려가 카드 조립이 깨질 수 있는 경우들이다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1MyBookmarkControllerTest {

    private static final String URL = "/api/v1/members/me/bookmarks";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private PartyRepository partyRepository;


    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    @Autowired
    private ContestService contestService;

    @Test
    @DisplayName("북마크함: 파티·대회·전시를 한 목록에 섞어 타입별 카드로 내려준다")
    @WithUserDetails("user1@test.com")
    void getMyBookmarks() throws Exception {
        Member actor = actor();

        bookmark(actor, TargetType.PARTY, saveParty().getId());
        bookmark(actor, TargetType.CONTEST, saveContest());
        long goalId = saveExhibitedGoal();
        long showcaseId = ((Project) goalRepository.findById(goalId).orElseThrow()).getPartyShowcase().getId();
        bookmark(actor, TargetType.PARTY_SHOWCASE, showcaseId);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                // 타입마다 target 모양이 다르다 - 각 목록 API 가 쓰는 카드 그대로다
                .andExpect(jsonPath("$.data.content[?(@.targetType=='PARTY')].target.partyName")
                        .value(contains("북마크 테스트용 파티")))
                .andExpect(jsonPath("$.data.content[?(@.targetType=='PARTY')].target.applicantCount")
                        .value(contains(0)))
                .andExpect(jsonPath("$.data.content[?(@.targetType=='CONTEST')].target.format")
                        .value(contains("HACKATHON")))
                .andExpect(jsonPath("$.data.content[?(@.targetType=='PARTY_SHOWCASE')].target.detail.title")
                        .value(contains("정산 자동화 API")))
                .andExpect(jsonPath("$.data.content[?(@.targetType=='PARTY_SHOWCASE')].target.source")
                        .value(contains("PLATFORM_VERIFIED")))
                .andExpect(jsonPath("$.data.content[?(@.targetType=='PARTY_SHOWCASE')].target.positionType")
                        .value(contains("BACK")))
                .andExpect(jsonPath("$.data.content[0].id").isNumber())
                .andExpect(jsonPath("$.data.content[0].bookmarkedAt").exists());
    }

    @Test
    @DisplayName("북마크함: 남이 담은 북마크는 섞이지 않는다")
    @WithUserDetails("user1@test.com")
    void getMyBookmarksExcludesOthers() throws Exception {
        Party party = saveParty();

        bookmark(actor(), TargetType.PARTY, party.getId());
        bookmark(memberRepository.findByEmail("user3@test.com").orElseThrow(),
                TargetType.PARTY, saveParty().getId());

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].target.id").value(party.getId()));
    }

    @Test
    @DisplayName("북마크함: 담은 게 없으면 빈 배열이다")
    @WithUserDetails("user1@test.com")
    void getMyBookmarksWhenEmpty() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("북마크함: size 로 페이지를 나눠 담은 순으로 끊어 준다")
    @WithUserDetails("user1@test.com")
    void getMyBookmarksPaged() throws Exception {
        Member actor = actor();

        for (int i = 0; i < 3; i++) {
            bookmark(actor, TargetType.PARTY, saveParty().getId());
        }

        mvc.perform(get(URL).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mvc.perform(get(URL).param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(1));
    }

    @Test
    @DisplayName("북마크함: size 가 범위를 벗어나면 400-1이다")
    @WithUserDetails("user1@test.com")
    void getMyBookmarksWithInvalidSize() throws Exception {
        mvc.perform(get(URL).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        mvc.perform(get(URL).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("북마크함: 로그인하지 않으면 401-1이다")
    void getMyBookmarksWithoutLogin() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("북마크함: 대상이 사라진 북마크는 500 대신 목록에서 빠진다")
    @WithUserDetails("user1@test.com")
    void getMyBookmarksSkipsDeletedTarget() throws Exception {
        Member actor = actor();
        Party party = saveParty();

        bookmark(actor, TargetType.PARTY, party.getId());
        // 대상 삭제 시 deleteAllBookmarksForTarget 이 정리하지만, 놓쳐서 남은 행을 흉내낸다.
        // 다형성 저장이라 FK 가 없어 이런 행이 실제로 남을 수 있다.
        bookmark(actor, TargetType.PARTY, 999_999L);
        bookmark(actor, TargetType.CONTEST, 999_999L);
        bookmark(actor, TargetType.PARTY_SHOWCASE, 999_999L);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].target.id").value(party.getId()));
    }

    /* ---------- 헬퍼 ---------- */

    private Member actor() {
        return memberRepository.findByEmail("user1@test.com").orElseThrow();
    }

    private void bookmark(Member member, TargetType targetType, long targetId) {
        bookmarkRepository.save(new Bookmark(member, targetType, targetId));
    }

    private Party saveParty() {
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        Party party = new Party(
                owner, "북마크 테스트용 파티", "북마크 테스트용 파티 모집", "설명",
                null, null, null, TopicType.PROJECT, PartyTag.WEB, null,
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 2));
        partyRepository.save(party);

        return party;
    }

    private long saveContest() {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        ContestResponseDto contest = contestService.write(
                admin, "북마크 테스트용 대회", ContestFormat.HACKATHON, ContestTag.AI,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                "설명", "https://example.com/contest", null
        );

        return contest.id();
    }

    /** 완료 + 파티장이 전시글까지 게시해서 전시 가능한 PROJECT */
    private long saveExhibitedGoal() {
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();
        Party party = saveParty();

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish("정산 자동화 API", "설명");
        partyShowcaseRepository.save(showcase);

        Project project = new Project(
                owner, null, party.getId(), "정산 자동화 API", PositionType.BACK, LocalDate.now());
        project.complete(LocalDate.now());
        project.linkShowcase(showcase);

        return goalRepository.save(project).getId();
    }

    @Test
    @DisplayName("북마크함: PROJECT 성취(PARTY_SHOWCASE로 저장된 북마크)도 500 없이 전시 카드로 노출된다")
    @WithUserDetails("user1@test.com")
    void getMyBookmarksIncludesPartyShowcaseBookmark() throws Exception {
        long goalId = saveExhibitedGoal();
        Project project = (Project) goalRepository.findById(goalId).orElseThrow();
        long showcaseId = project.getPartyShowcase().getId();

        bookmark(actor(), TargetType.PARTY_SHOWCASE, showcaseId);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].targetType").value("PARTY_SHOWCASE"))
                .andExpect(jsonPath("$.data.content[0].target.detail.title").value("정산 자동화 API"));
    }
}
