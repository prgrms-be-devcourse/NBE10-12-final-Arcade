package com.back.domain.contest.contest.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1ContestControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContestService contestService;

    @Autowired
    private LikeActionRepository likeActionRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private ContestPostRepository contestPostRepository;

    @Autowired
    private PartyRepository partyRepository;

    private long savePartyForContest(long contestId, String partyName) {
        Member owner = memberRepository.findByEmail("admin").orElseThrow();

        Party party = new Party(
                owner,
                partyName,
                "파티 제목",
                "설명",
                contestId,
                null,
                null,
                TopicType.CONTEST,
                PartyTag.WEB,
                null,
                1,
                LocalDateTime.now().plusDays(7)
        );

        return partyRepository.save(party).getId();
    }

    private long writeContestAsAdmin(String title) {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        ContestResponseDto contest = contestService.write(
                admin,
                title,
                ContestFormat.HACKATHON,
                ContestTag.AI,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "설명",
                "https://example.com/contest",
                null
        );

        return contest.id();
    }

    private String modifyRequestJson(String title, String linkUrl) {
        return """
            {
                "title": "%s",
                "description": "수정된 대회 설명입니다",
                "applicationPeriodStart": "2026-09-05",
                "applicationPeriodEnd": "2026-10-05",
                "linkUrl": "%s"
            }
            """.formatted(title, linkUrl);
    }

    private String writeRequestJson(String title, String linkUrl) {
        return """
            {
                "title": "%s",
                "format": "HACKATHON",
                "contestTag": "AI",
                "applicationPeriodStart": "2026-09-01",
                "applicationPeriodEnd": "2026-09-30",
                "description": "이것은 테스트용 대회 설명입니다",
                "linkUrl": "%s"
            }
            """.formatted(title, linkUrl);
    }

    @Test
    @DisplayName("대회 등록: 관리자가 등록하면 201-1과 등록된 대회를 반환한다")
    @WithUserDetails("admin")
    void writeByAdmin() throws Exception {
        long adminId = memberRepository.findByEmail("admin").orElseThrow().getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/contests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("AI 해커톤", "https://example.com/contest")));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("공모전 등록 성공"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.creatorMemberId").value(adminId))
                .andExpect(jsonPath("$.data.title").value("AI 해커톤"))
                .andExpect(jsonPath("$.data.format").value("HACKATHON"))
                .andExpect(jsonPath("$.data.contestTag").value("AI"))
                .andExpect(jsonPath("$.data.linkUrl").value("https://example.com/contest"))
                .andExpect(jsonPath("$.data.archived").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.viewCount").value(0));
    }

    @Test
    @DisplayName("대회 등록: 관리자가 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void writeByNonAdmin() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/contests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("AI 해커톤", "https://example.com/contest")));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("대회 등록: 로그인하지 않았으면 401-1이다")
    void writeWithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/contests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("AI 해커톤", "https://example.com/contest")));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("대회 등록: 제목이 없으면 400-1이다")
    @WithUserDetails("admin")
    void writeWithoutTitle() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/contests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("", "https://example.com/contest")));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("대회 등록: 모집 시작일이 종료일보다 늦으면 400-3이다")
    @WithUserDetails("admin")
    void writeWithStartAfterEnd() throws Exception {
        String body = """
            {
                "title": "기간 역전 대회",
                "format": "HACKATHON",
                "contestTag": "AI",
                "applicationPeriodStart": "2026-10-01",
                "applicationPeriodEnd": "2026-09-01",
                "description": "이것은 테스트용 대회 설명입니다",
                "linkUrl": "https://example.com/contest"
            }
            """;

        ResultActions resultActions = mvc.perform(post("/api/v1/contests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));
    }

    @Test
    @DisplayName("대회 수정: 관리자가 수정하면 200-1과 수정된 대회를 반환한다")
    @WithUserDetails("admin")
    void modifyByAdmin() throws Exception {
        long contestId = writeContestAsAdmin("수정 전 제목");

        ResultActions resultActions = mvc.perform(patch("/api/v1/contests/" + contestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(modifyRequestJson("수정된 제목", "https://example.com/modified")));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.id").value(contestId))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.description").value("수정된 대회 설명입니다"))
                .andExpect(jsonPath("$.data.linkUrl").value("https://example.com/modified"))
                .andExpect(jsonPath("$.data.applicationPeriodStart").value("2026-09-05"))
                .andExpect(jsonPath("$.data.applicationPeriodEnd").value("2026-10-05"));
    }

    @Test
    @DisplayName("대회 수정: 관리자가 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void modifyByNonAdmin() throws Exception {
        long contestId = writeContestAsAdmin("수정 전 제목");

        ResultActions resultActions = mvc.perform(patch("/api/v1/contests/" + contestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(modifyRequestJson("수정된 제목", "https://example.com/modified")));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("대회 수정: 제목이 없으면 400-1이다")
    @WithUserDetails("admin")
    void modifyWithoutTitle() throws Exception {
        long contestId = writeContestAsAdmin("수정 전 제목");

        ResultActions resultActions = mvc.perform(patch("/api/v1/contests/" + contestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(modifyRequestJson("", "https://example.com/modified")));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("대회 수정: 모집 시작일이 종료일보다 늦으면 400-3이다")
    @WithUserDetails("admin")
    void modifyWithStartAfterEnd() throws Exception {
        long contestId = writeContestAsAdmin("수정 전 제목");

        String body = """
            {
                "title": "수정된 제목",
                "description": "수정된 대회 설명입니다",
                "applicationPeriodStart": "2026-10-05",
                "applicationPeriodEnd": "2026-09-05",
                "linkUrl": "https://example.com/modified"
            }
            """;

        ResultActions resultActions = mvc.perform(patch("/api/v1/contests/" + contestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));
    }

    @Test
    @DisplayName("대회 수정: 존재하지 않는 대회면 404-1이다")
    @WithUserDetails("admin")
    void modifyNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(patch("/api/v1/contests/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(modifyRequestJson("수정된 제목", "https://example.com/modified")));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("대회 삭제: 관리자가 삭제하면 204-1이다")
    @WithUserDetails("admin")
    void deleteByAdmin() throws Exception {
        long contestId = writeContestAsAdmin("삭제될 대회");

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"));
    }

    @Test
    @DisplayName("대회 삭제: 관리자가 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void deleteByNonAdmin() throws Exception {
        long contestId = writeContestAsAdmin("삭제될 대회");

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("대회 삭제: 존재하지 않는 대회면 404-1이다")
    @WithUserDetails("admin")
    void deleteNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/999999"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("대회 삭제: 이미 삭제된 게시글에 다시 호출해도 204-1이다")
    @WithUserDetails("admin")
    void deleteTwiceIsIdempotent() throws Exception {
        long contestId = writeContestAsAdmin("삭제될 대회");

        mvc.perform(delete("/api/v1/contests/" + contestId))
                .andExpect(status().isNoContent());

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"));
    }

    @Test
    @DisplayName("대회 삭제: 좋아요/북마크도 함께 삭제된다")
    @WithUserDetails("admin")
    void deletingContestCascadesLikesAndBookmarks() throws Exception {
        long contestId = writeContestAsAdmin("좋아요북마크 함께삭제될 대회");

        mvc.perform(post("/api/v1/contests/" + contestId + "/likes")).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks")).andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/contests/" + contestId)).andExpect(status().isNoContent());

        Member admin = memberRepository.findByEmail("admin").orElseThrow();
        Assertions.assertFalse(likeActionRepository.existsByMemberAndTargetTypeAndTargetId(admin, TargetType.CONTEST, contestId));
        Assertions.assertFalse(bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(admin, TargetType.CONTEST, contestId));
    }

    @Test
    @DisplayName("대회 삭제: viewCount/likeCount가 쌓인 상태여도 게시글 행 자체가 삭제되어 카운트가 남지 않는다")
    @WithUserDetails("admin")
    void deletingContestRemovesAccumulatedCounts() throws Exception {
        long contestId = writeContestAsAdmin("카운트 쌓인 뒤 삭제될 대회");

        mvc.perform(get("/api/v1/contests/" + contestId)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/contests/" + contestId + "/likes")).andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/contests/" + contestId)).andExpect(status().isNoContent());

        Contest contest = contestRepository.findById(contestId).orElseThrow();
        Assertions.assertTrue(contestPostRepository.findByContest(contest).isEmpty());
    }

    @Test
    @DisplayName("대회 목록 조회: 등록된 대회가 목록에 포함된다")
    void listReturnsRegisteredContests() throws Exception {
        writeContestAsAdmin("목록 조회용 대회");

        ResultActions resultActions = mvc.perform(get("/api/v1/contests"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content[?(@.title == '목록 조회용 대회')]").exists());
    }

    @Test
    @DisplayName("대회 목록 조회: 내가 북마크한 대회는 bookmarkedByMe=true, 안 한 대회는 false다")
    @WithUserDetails("admin")
    void listReflectsBookmarkedByMe() throws Exception {
        long bookmarkedId = writeContestAsAdmin("내가 북마크한 대회");
        long notBookmarkedId = writeContestAsAdmin("북마크 안 한 대회");

        mvc.perform(post("/api/v1/contests/" + bookmarkedId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(get("/api/v1/contests"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + bookmarkedId + ")].bookmarkedByMe").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + notBookmarkedId + ")].bookmarkedByMe").value(false));
    }

    @Test
    @DisplayName("대회 목록 조회: 내가 좋아요한 대회는 likedByMe=true, 안 한 대회는 false다")
    @WithUserDetails("admin")
    void listReflectsLikedByMe() throws Exception {
        long likedId = writeContestAsAdmin("내가 좋아요한 대회");
        long notLikedId = writeContestAsAdmin("좋아요 안 한 대회");

        mvc.perform(post("/api/v1/contests/" + likedId + "/likes"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(get("/api/v1/contests"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + likedId + ")].likedByMe").value(true))
                .andExpect(jsonPath("$.data.content[?(@.id == " + notLikedId + ")].likedByMe").value(false));
    }

    @Test
    @DisplayName("대회 목록 조회: format으로 필터링한다")
    void listFilteredByFormat() throws Exception {
        writeContestAsAdmin("해커톤 대회");

        ResultActions resultActions = mvc.perform(get("/api/v1/contests")
                .param("format", "CONTEST"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.title == '해커톤 대회')]").doesNotExist());
    }

    @Test
    @DisplayName("대회 목록 조회: 게시글이 삭제된(archived) 대회는 목록에서 제외된다")
    @WithUserDetails("admin")
    void listExcludesArchivedContest() throws Exception {
        long contestId = writeContestAsAdmin("삭제되어 제외될 대회");
        mvc.perform(delete("/api/v1/contests/" + contestId)).andExpect(status().isNoContent());

        ResultActions resultActions = mvc.perform(get("/api/v1/contests"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.title == '삭제되어 제외될 대회')]").doesNotExist());
    }

    @Test
    @DisplayName("대회 목록 조회: 마감임박순(DEADLINE) 정렬은 applicationPeriodEnd 오름차순이다")
    void listSortedByDeadline() throws Exception {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        contestService.write(admin, "늦게 마감", ContestFormat.HACKATHON, ContestTag.AI,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31),
                "설명", "https://example.com/late", null);
        contestService.write(admin, "빨리 마감", ContestFormat.HACKATHON, ContestTag.AI,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10),
                "설명", "https://example.com/soon", null);

        ResultActions resultActions = mvc.perform(get("/api/v1/contests")
                .param("sort", "DEADLINE"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("빨리 마감"))
                .andExpect(jsonPath("$.data.content[1].title").value("늦게 마감"));
    }

    @Test
    @DisplayName("대회 목록 조회: sortOption이 null이면 LATEST로 대체되어 정상 조회된다")
    void listWithNullSortOptionFallsBackToLatest() {
        contestService.write(
                memberRepository.findByEmail("admin").orElseThrow(),
                "정렬 null 테스트 대회", ContestFormat.HACKATHON, ContestTag.AI,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                "설명", "https://example.com/null-sort", null
        );

        var result = contestService.list(null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));

        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("대회 상세 조회: 로그인 없이 조회하면 200-1과 대회 정보를 반환하고 viewCount가 증가한다")
    void getDetailIncreasesViewCount() throws Exception {
        long contestId = writeContestAsAdmin("조회될 대회");

        mvc.perform(get("/api/v1/contests/" + contestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.id").value(contestId))
                .andExpect(jsonPath("$.data.archived").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mvc.perform(get("/api/v1/contests/" + contestId))
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }

    @Test
    @DisplayName("대회 상세 조회: 같은 방문자(쿠키 보유)가 24시간 내 다시 조회하면 viewCount가 증가하지 않는다")
    void getDetailWithViewCookieDoesNotIncreaseViewCount() throws Exception {
        long contestId = writeContestAsAdmin("조회될 대회");

        ResultActions first = mvc.perform(get("/api/v1/contests/" + contestId));
        first.andExpect(jsonPath("$.data.viewCount").value(1));

        Cookie viewCookie = first.andReturn().getResponse().getCookie("contest_viewed_" + contestId);
        Assertions.assertNotNull(viewCookie);

        mvc.perform(get("/api/v1/contests/" + contestId).cookie(viewCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));
    }

    @Test
    @DisplayName("대회 상세 조회: 게시글이 삭제된 대회는 archived=true이고 게시글 필드가 null이다")
    @WithUserDetails("admin")
    void getDetailAfterPostDeleted() throws Exception {
        long contestId = writeContestAsAdmin("삭제될 대회");
        mvc.perform(delete("/api/v1/contests/" + contestId)).andExpect(status().isNoContent());

        ResultActions resultActions = mvc.perform(get("/api/v1/contests/" + contestId));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived").value(true))
                .andExpect(jsonPath("$.data.description").doesNotExist())
                .andExpect(jsonPath("$.data.linkUrl").doesNotExist())
                .andExpect(jsonPath("$.data.likeCount").doesNotExist())
                .andExpect(jsonPath("$.data.viewCount").doesNotExist());
    }

    @Test
    @DisplayName("대회 상세 조회: 존재하지 않는 대회면 404-1이다")
    void getDetailNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/contests/999999"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("만료 게시글 자동 삭제: applicationPeriodEnd가 지난 대회의 게시글이 스케줄러 실행 시 삭제된다")
    void deleteExpiredPostsRemovesExpiredContestPost() throws Exception {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        ContestResponseDto expired = contestService.write(
                admin,
                "기한 지난 대회",
                ContestFormat.HACKATHON,
                ContestTag.AI,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1),
                "설명",
                "https://example.com/expired",
                null
        );

        ContestResponseDto notExpired = contestService.write(
                admin,
                "기한 안 지난 대회",
                ContestFormat.HACKATHON,
                ContestTag.AI,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(10),
                "설명",
                "https://example.com/not-expired",
                null
        );

        contestService.deleteExpiredPosts();

        mvc.perform(get("/api/v1/contests/" + expired.id()))
                .andExpect(jsonPath("$.data.archived").value(true));

        mvc.perform(get("/api/v1/contests/" + notExpired.id()))
                .andExpect(jsonPath("$.data.archived").value(false));
    }

    @Test
    @DisplayName("대회 목록 조회: 대회를 타겟으로 하는 파티 수가 teams에 반영된다")
    @WithUserDetails("admin")
    void listReflectsPartyTeamsCount() throws Exception {
        long contestId = writeContestAsAdmin("파티 두 개 달린 대회");
        long noPartyContestId = writeContestAsAdmin("파티 없는 대회");

        savePartyForContest(contestId, "파티 A");
        savePartyForContest(contestId, "파티 B");

        ResultActions resultActions = mvc.perform(get("/api/v1/contests"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + contestId + ")].teams").value(2))
                .andExpect(jsonPath("$.data.content[?(@.id == " + noPartyContestId + ")].teams").value(0));
    }

    @Test
    @DisplayName("대회 상세 조회: 대회를 타겟으로 하는 파티 목록이 relatedParties에 반환된다")
    @WithUserDetails("admin")
    void getDetailReflectsRelatedParties() throws Exception {
        long contestId = writeContestAsAdmin("상세에서 파티 보여줄 대회");
        savePartyForContest(contestId, "연결된 파티");

        ResultActions resultActions = mvc.perform(get("/api/v1/contests/" + contestId));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams").value(1))
                .andExpect(jsonPath("$.data.relatedParties[0].partyName").value("연결된 파티"));
    }

    @Test
    @DisplayName("대회 상세 조회: 연결된 파티가 없으면 teams=0, relatedParties가 빈 배열이다")
    void getDetailWithoutPartiesHasZeroTeamsAndEmptyRelatedParties() throws Exception {
        long contestId = writeContestAsAdmin("파티 없는 대회");

        ResultActions resultActions = mvc.perform(get("/api/v1/contests/" + contestId));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teams").value(0))
                .andExpect(jsonPath("$.data.relatedParties").isArray())
                .andExpect(jsonPath("$.data.relatedParties").isEmpty());
    }
}
