package com.back.domain.goal.goal.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1GoalControllerTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions createGoal(String body) throws Exception {
        return mvc.perform(
                post("/api/v1/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );
    }

    @Test
    @DisplayName("자기신고 등록: 수상·대회 성취를 SELF_REPORTED로 등록한다")
    @WithUserDetails("user1@test.com")
    void createContestGoal() throws Exception {
        ResultActions resultActions = createGoal("""
                {
                  "type": "CONTEST",
                  "status": "ACHIEVED",
                  "detail": {
                    "contestName": "사내 해커톤",
                    "isTeam": true,
                    "result": "장려상",
                    "awardDate": "2023-11-15"
                  }
                }
                """);

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("성취 등록 성공"))
                .andExpect(jsonPath("$.data.type").value("CONTEST"))
                .andExpect(jsonPath("$.data.status").value("ACHIEVED"))
                .andExpect(jsonPath("$.data.source").value("SELF_REPORTED"))
                .andExpect(jsonPath("$.data.detail.contestName").value("사내 해커톤"))
                .andExpect(jsonPath("$.data.detail.isTeam").value(true))
                .andExpect(jsonPath("$.data.detail.result").value("장려상"))
                .andExpect(jsonPath("$.data.detail.awardDate").value("2023-11-15"));
    }

    @Test
    @DisplayName("자기신고 등록: 이미 끝난 활동이 아니어도 WANT 상태로 등록할 수 있다")
    @WithUserDetails("user1@test.com")
    void createChecklistGoal() throws Exception {
        ResultActions resultActions = createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": {
                    "title": "정보처리기사 실기 준비",
                    "memo": "매주 토요일 2시간",
                    "targetDate": "2026-10-01"
                  }
                }
                """);

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("CHECKLIST"))
                .andExpect(jsonPath("$.data.status").value("WANT"))
                .andExpect(jsonPath("$.data.detail.title").value("정보처리기사 실기 준비"))
                .andExpect(jsonPath("$.data.detail.memo").value("매주 토요일 2시간"))
                .andExpect(jsonPath("$.data.viewCount").value(0));
    }

    @Test
    @DisplayName("자기신고 등록: PROJECT는 자동생성 전용이라 400-4로 거부된다")
    @WithUserDetails("user1@test.com")
    void createProjectGoalIsRejected() throws Exception {
        ResultActions resultActions = createGoal("""
                {
                  "type": "PROJECT",
                  "status": "IN_PROGRESS",
                  "detail": { "title": "사이드 프로젝트" }
                }
                """);

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    @DisplayName("자기신고 등록: 대회명이 없으면 400-4로 거부된다")
    @WithUserDetails("user1@test.com")
    void createContestGoalWithoutContestName() throws Exception {
        ResultActions resultActions = createGoal("""
                {
                  "type": "CONTEST",
                  "status": "ACHIEVED",
                  "detail": { "result": "대상" }
                }
                """);

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    @DisplayName("자기신고 등록: type이 없으면 400-1로 거부된다")
    @WithUserDetails("user1@test.com")
    void createGoalWithoutType() throws Exception {
        ResultActions resultActions = createGoal("""
                {
                  "status": "WANT",
                  "detail": { "title": "제목" }
                }
                """);

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("자기신고 등록: 비로그인이면 401-1이다")
    void createGoalWithoutLogin() throws Exception {
        ResultActions resultActions = createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "제목" }
                }
                """);

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
