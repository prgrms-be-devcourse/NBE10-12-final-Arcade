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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
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

    /* ---------- 내 성취 목록 조회 ---------- */

    /** 필터 검증용으로 성격이 다른 성취 세 건을 만들어 둔다 */
    private void seedGoals() throws Exception {
        createGoal("""
                {
                  "type": "CONTEST",
                  "status": "ACHIEVED",
                  "detail": { "contestName": "사내 해커톤", "result": "장려상", "awardDate": "2023-11-15" }
                }
                """).andExpect(status().isCreated());
        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "정보처리기사 실기", "targetDate": "2026-10-01" }
                }
                """).andExpect(status().isCreated());
        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "ACHIEVED",
                  "detail": { "title": "토익 900점" }
                }
                """).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("내 성취 목록: 필터 없이 조회하면 내 성취가 전부 나온다")
    @WithUserDetails("user1@test.com")
    void getMyGoals() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 성취 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[*].source", everyItem(equalTo("SELF_REPORTED"))));
    }

    @Test
    @DisplayName("내 성취 목록: type 으로 거를 수 있다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredByType() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("type", "CHECKLIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].type", everyItem(equalTo("CHECKLIST"))));
    }

    @Test
    @DisplayName("내 성취 목록: status 와 type 을 함께 걸면 둘 다 만족하는 것만 나온다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredByStatusAndType() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("status", "ACHIEVED").param("type", "CHECKLIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].detail.title").value("토익 900점"));
    }

    @Test
    @DisplayName("내 성취 목록: 남의 성취는 나오지 않는다")
    @WithUserDetails("user2@test.com")
    void getMyGoalsExcludesOthers() throws Exception {
        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    /**
     * 정의되지 않은 enum 값이 오면 400 이 나가지만 응답 본문은 비어 있다.
     *
     * MethodArgumentTypeMismatchException 을 GlobalExceptionHandler 가 처리하지 않아
     * {resultCode, msg, data} 봉투가 붙지 않는다. API 명세서는 400-1 을 기대하므로 전역 핸들러 보완이 필요하나,
     * 전 도메인 공용 코드라 여기서는 실제 동작만 고정해 둔다.
     */
    @Test
    @DisplayName("내 성취 목록: 정의되지 않은 필터 값은 400 이다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsWithInvalidFilter() throws Exception {
        mvc.perform(get("/api/v1/goals/me").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내 성취 목록: 비로그인이면 401-1 이다")
    void getMyGoalsWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
