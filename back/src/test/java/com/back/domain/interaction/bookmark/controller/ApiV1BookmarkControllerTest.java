package com.back.domain.interaction.bookmark.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1BookmarkControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContestService contestService;

    private long saveContest() {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        ContestResponseDto contest = contestService.write(
                admin,
                "북마크 테스트용 대회",
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

    @Test
    @DisplayName("대회 북마크: 201-1과 bookmarked=true를 반환한다")
    @WithUserDetails("user1@test.com")
    void bookmarkContest() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.targetType").value("CONTEST"))
                .andExpect(jsonPath("$.data.targetId").value(contestId))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("대회 북마크: 이미 북마크한 대회에 재요청하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkContestTwice() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("대회 북마크: 존재하지 않는 대회면 404-1이다")
    @WithUserDetails("user1@test.com")
    void bookmarkContestNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/contests/999999/bookmarks"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("대회 북마크 취소: 204-1")
    @WithUserDetails("user1@test.com")
    void unbookmarkContest() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("대회 북마크 취소: 북마크하지 않은 대회를 취소하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void unbookmarkContestWithoutBookmarking() throws Exception {
        long contestId = saveContest();

        ResultActions resultActions = mvc.perform(delete("/api/v1/contests/" + contestId + "/bookmarks"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("대회 북마크: 북마크→취소→재북마크를 반복해도 매번 정상 토글된다")
    @WithUserDetails("user1@test.com")
    void bookmarkContestToggleCycle() throws Exception {
        long contestId = saveContest();

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        mvc.perform(delete("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/contests/" + contestId + "/bookmarks"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }
}
