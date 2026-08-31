package com.back.domain.contest.contest.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
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
                "description": "수정된 설명",
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
                .andExpect(jsonPath("$.data.description").value("수정된 설명"))
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
    @DisplayName("대회 목록 조회: 등록된 대회가 목록에 포함된다")
    void listReturnsRegisteredContests() throws Exception {
        writeContestAsAdmin("목록 조회용 대회");

        ResultActions resultActions = mvc.perform(get("/api/v1/contests"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content[?(@.title == '목록 조회용 대회')]").exists());
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
    @DisplayName("대회 상세 조회: 로그인 없이 조회하면 200-1과 대회 정보를 반환하고 viewCount가 증가한다")
    void getDetailIncreasesViewCount() throws Exception {
        long contestId = writeContestAsAdmin("조회될 대회");

        mvc.perform(get("/api/v1/contests/" + contestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.id").value(contestId))
                .andExpect(jsonPath("$.data.archived").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        // 쿠키를 안 보내는 별개 요청 = 다른 방문자로 취급되어 계속 증가한다
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
}
