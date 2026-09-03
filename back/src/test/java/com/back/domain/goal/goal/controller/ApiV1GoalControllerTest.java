package com.back.domain.goal.goal.controller;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.PersonalTodoItem;
import org.springframework.data.domain.PageRequest;
import com.back.domain.todo.todo.entity.TodoCategory;
import com.back.domain.todo.todo.repository.PersonalTodoItemRepository;
import com.back.domain.todo.todo.repository.PersonalTodoRepository;
import jakarta.persistence.EntityManager;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
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

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PersonalTodoRepository personalTodoRepository;

    @Autowired
    private PersonalTodoItemRepository personalTodoItemRepository;

    @Autowired
    private EntityManager em;

    /** 연결 대상 개인 TODO - 화면과 같은 흐름으로 TODO 를 만들고 항목을 한 건씩 붙인다 */
    private PersonalTodo saveTodoOf(String email, String... contents) {
        Member owner = memberRepository.findByEmail(email).orElseThrow();
        PersonalTodo todo = personalTodoRepository.save(
                new PersonalTodo(owner, "정보처리기사 실기 준비", TodoCategory.STUDY, "매주 토요일 2시간")
        );

        for (String content : contents) {
            personalTodoItemRepository.save(new PersonalTodoItem(
                    todo, content, personalTodoItemRepository.nextSortOrder(todo)
            ));
        }

        return todo;
    }

    /** PROJECT 성취 상세 검증용 - 파티를 만들고 그 파티를 가리키는 자동기록 성취를 심는다 */
    private Project savePartyAndProjectGoal(String ownerEmail) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();

        Party party = new Party(
                owner, "오락실 팀", "오락실 공모전 팀원 모집", "설명",
                null, null, null,
                TopicType.PROJECT, PartyTag.WEB,
                "https://github.com/crewon/oakroom", 1,
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 3));
        Party saved = partyRepository.save(party);

        return goalRepository.save(new Project(
                owner, null, saved.getId(), null, null, LocalDate.of(2026, 9, 1)
        ));
    }

    /** 소유자 검증용 - 지정한 회원 이름으로 성취를 하나 심는다 */
    private long saveChecklistGoalOf(String email, String title) {
        Member owner = memberRepository.findByEmail(email).orElseThrow();
        return goalRepository
                .save(new PersonalChecklist(owner, GoalStatus.WANT, title, null, null))
                .getId();
    }

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
                .andExpect(jsonPath("$.data.detail.memo").value("매주 토요일 2시간"));
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
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].source", everyItem(equalTo("SELF_REPORTED"))));
    }

    @Test
    @DisplayName("내 성취 목록: type 으로 거를 수 있다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredByType() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("type", "CHECKLIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].type", everyItem(equalTo("CHECKLIST"))));
    }

    @Test
    @DisplayName("내 성취 목록: status 와 type 을 함께 걸면 둘 다 만족하는 것만 나온다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredByStatusAndType() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("status", "ACHIEVED").param("type", "CHECKLIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].detail.title").value("토익 900점"));
    }

    @Test
    @DisplayName("내 성취 목록: 최신순으로 온다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsIsOrderedByNewestFirst() throws Exception {
        seedGoals();   // 사내 해커톤 → 정보처리기사 실기 → 토익 900점 순으로 등록된다

        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].detail.title").value("토익 900점"))
                .andExpect(jsonPath("$.data[1].detail.title").value("정보처리기사 실기"))
                .andExpect(jsonPath("$.data[2].detail.contestName").value("사내 해커톤"));
    }

    /**
     * 생성 시각이 같은 행끼리도 순서가 정해져야 한다.
     *
     * createDate 하나로만 정렬하면 같은 시각의 행들 순서가 조회할 때마다 달라질 수 있다.
     * 그래서 id 를 보조 정렬키로 두었고, 이 테스트가 그 보조키를 지킨다.
     * (한 번의 요청으로 여러 성취가 만들어지는 파티 확정 자동생성이 실제로 이 상황을 만든다.)
     */
    @Test
    @DisplayName("내 성취 목록: 생성 시각이 같으면 나중에 만들어진 것이 먼저 온다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsBreaksTieByIdDescending() throws Exception {
        long first = saveChecklistGoalOf("user1@test.com", "먼저 만든 것");
        long second = saveChecklistGoalOf("user1@test.com", "나중에 만든 것");

        // 두 성취의 createDate 를 같은 값으로 맞춰 보조 정렬키만 남긴다
        em.flush();
        em.createNativeQuery("update goal set create_date = timestamp '2026-01-01 00:00:00' where id in (:ids)")
                .setParameter("ids", List.of(first, second))
                .executeUpdate();
        em.clear();

        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value((int) second))
                .andExpect(jsonPath("$.data[1].id").value((int) first));
    }

    /**
     * 출처 필터 — 자기신고(SELF_REPORTED)와 파티 활동 자동기록(PLATFORM_VERIFIED)을 갈라 본다.
     * 마이페이지 연혁 filterBlock 의 '출처' 칩에 대응한다.
     */
    @Test
    @DisplayName("내 성취 목록: source 로 자기신고와 자동기록을 갈라 볼 수 있다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredBySource() throws Exception {
        seedGoals();                                // 자기신고 3건
        savePartyAndProjectGoal("user1@test.com");  // 자동기록 1건

        mvc.perform(get("/api/v1/goals/me").param("source", "SELF_REPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].source", everyItem(equalTo("SELF_REPORTED"))));

        mvc.perform(get("/api/v1/goals/me").param("source", "PLATFORM_VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].source").value("PLATFORM_VERIFIED"))
                .andExpect(jsonPath("$.data[0].type").value("PROJECT"));

        // 필터를 빼면 둘 다 나온다
        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    @DisplayName("내 성취 목록: source 와 다른 필터를 함께 걸 수 있다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredBySourceAndOthers() throws Exception {
        seedGoals();
        savePartyAndProjectGoal("user1@test.com");

        // 자동기록 + CHECKLIST 는 성립할 수 없는 조합이라 빈 결과
        mvc.perform(get("/api/v1/goals/me")
                        .param("source", "PLATFORM_VERIFIED")
                        .param("type", "CHECKLIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 자기신고 + 달성
        mvc.perform(get("/api/v1/goals/me")
                        .param("source", "SELF_REPORTED")
                        .param("status", "ACHIEVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].source", everyItem(equalTo("SELF_REPORTED"))));
    }

    @Test
    @DisplayName("내 성취 목록: 남의 성취는 나오지 않는다")
    @WithUserDetails("user2@test.com")
    void getMyGoalsExcludesOthers() throws Exception {
        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("내 성취 목록: 정의되지 않은 필터 값은 400-1 이다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsWithInvalidFilter() throws Exception {
        mvc.perform(get("/api/v1/goals/me").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("내 성취 목록: 비로그인이면 401-1 이다")
    void getMyGoalsWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    /* ---------- 성취 상세 조회 ---------- */

    @Test
    @DisplayName("성취 상세: 본인 성취를 조회하면 detail 까지 채워져 온다")
    @WithUserDetails("user1@test.com")
    void getGoal() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        mvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("성취 상세 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(goalId))
                .andExpect(jsonPath("$.data.type").value("CHECKLIST"))
                .andExpect(jsonPath("$.data.source").value("SELF_REPORTED"))
                .andExpect(jsonPath("$.data.detail.title").value("정보처리기사 실기"));
    }

    /** 성취는 전체 공개라 남의 것도 볼 수 있다(기획서 2.5, 3.7) */
    @Test
    @DisplayName("성취 상세: 남의 성취도 조회할 수 있다")
    @WithUserDetails("user2@test.com")
    void getGoalOfOthers() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "남의 목표");

        mvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.detail.title").value("남의 목표"));
    }

    @Test
    @DisplayName("성취 상세: 없는 id 는 404-1 이다")
    @WithUserDetails("user1@test.com")
    void getGoalNotFound() throws Exception {
        mvc.perform(get("/api/v1/goals/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 상세: /goals/me 가 상세 경로에 먹히지 않는다")
    @WithUserDetails("user1@test.com")
    void meIsNotTreatedAsGoalId() throws Exception {
        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("내 성취 목록 조회 성공"));
    }

    @Test
    @DisplayName("성취 상세: PROJECT 는 가리키는 파티 정보가 함께 온다")
    @WithUserDetails("user1@test.com")
    void getProjectGoalWithPartyContext() throws Exception {
        Project project = savePartyAndProjectGoal("user1@test.com");

        mvc.perform(get("/api/v1/goals/" + project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("PROJECT"))
                .andExpect(jsonPath("$.data.source").value("PLATFORM_VERIFIED"))
                .andExpect(jsonPath("$.data.project.partyId").value(project.getSourcePartyId()))
                .andExpect(jsonPath("$.data.project.partyName").value("오락실 팀"))
                .andExpect(jsonPath("$.data.project.githubRepoUrl").value("https://github.com/crewon/oakroom"))
                // 파티를 만든 사람이 곧 이 성취의 주인이라 파티장으로 표시된다
                .andExpect(jsonPath("$.data.project.partyOwner").value(true))
                .andExpect(jsonPath("$.data.project.pullRequests").isArray());
    }

    @Test
    @DisplayName("성취 상세: 남의 PROJECT 성취를 보면 PR 목록은 비어 온다")
    @WithUserDetails("user2@test.com")
    void getOthersProjectGoalHidesPullRequests() throws Exception {
        Project project = savePartyAndProjectGoal("user1@test.com");

        mvc.perform(get("/api/v1/goals/" + project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project.partyName").value("오락실 팀"))
                .andExpect(jsonPath("$.data.project.pullRequests", hasSize(0)));
    }

    @Test
    @DisplayName("성취 상세: PROJECT 가 아니면 project 필드가 없다")
    @WithUserDetails("user1@test.com")
    void nonProjectGoalHasNoPartyContext() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        mvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project").doesNotExist());
    }

    /* ---------- 내 성취 검색 (연도 · 키워드) ---------- */

    @Test
    @DisplayName("내 성취 검색: year 로 거르면 그 연도의 성취만 나온다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredByYear() throws Exception {
        seedGoals();

        // 수상일 2023-11-15 인 대회 성취 한 건
        mvc.perform(get("/api/v1/goals/me").param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].detail.contestName").value("사내 해커톤"));
    }

    /**
     * 연도의 기준 날짜는 타입마다 다르다 - PROJECT 는 참여 시작일, CONTEST 는 수상일, CHECKLIST 는 목표일.
     * 마이페이지 연혁이 연도별로 묶는 기준과 같아야 하므로 함께 고정해 둔다.
     */
    @Test
    @DisplayName("내 성취 검색: 연도 기준 날짜는 타입마다 다르다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsYearUsesTypeSpecificDate() throws Exception {
        // 타입마다 다른 컬럼에서 연도를 읽는지 보려는 것이라, 세 건 모두 자기 날짜를 갖게 하고
        // 등록일로 떨어지는 경우(아래 별도 테스트)와 섞이지 않게 서로 다른 연도를 준다.
        createGoal("""
                {
                  "type": "CONTEST",
                  "status": "ACHIEVED",
                  "detail": { "contestName": "사내 해커톤", "awardDate": "2023-11-15" }
                }
                """).andExpect(status().isCreated());
        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "정보처리기사 실기", "targetDate": "2024-10-01" }
                }
                """).andExpect(status().isCreated());
        savePartyAndProjectGoal("user1@test.com");   // startDate 2026-09-01

        // CONTEST 는 수상일
        mvc.perform(get("/api/v1/goals/me").param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("CONTEST"));

        // CHECKLIST 는 목표일
        mvc.perform(get("/api/v1/goals/me").param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("CHECKLIST"));

        // PROJECT 는 참여 시작일
        mvc.perform(get("/api/v1/goals/me").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("PROJECT"));
    }

    /**
     * 날짜가 없는 성취는 등록일 연도로 잡힌다.
     * '토익 900점' 은 targetDate 를 넣지 않았으므로 이 테스트가 도는 해에 걸린다.
     */
    @Test
    @DisplayName("내 성취 검색: 날짜가 없는 성취는 등록일 연도로 잡힌다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsYearFallsBackToCreateDate() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("year", String.valueOf(LocalDate.now().getYear())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].detail.title", hasItem("토익 900점")));
    }

    @Test
    @DisplayName("내 성취 검색: keyword 는 제목과 대회명을 함께 훑는다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsSearchedByKeyword() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("keyword", "해커톤"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].detail.contestName").value("사내 해커톤"));

        mvc.perform(get("/api/v1/goals/me").param("keyword", "정보처리"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].detail.title").value("정보처리기사 실기"));
    }

    @Test
    @DisplayName("내 성취 검색: keyword 는 수상 결과·메모 같은 설명도 훑는다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsSearchedByKeywordInDescription() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("keyword", "장려상"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].detail.contestName").value("사내 해커톤"));
    }

    @Test
    @DisplayName("내 성취 검색: keyword 는 대소문자를 구분하지 않는다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsSearchedByKeywordIgnoringCase() throws Exception {
        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "AWS SAA 취득" }
                }
                """).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/goals/me").param("keyword", "aws"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("내 성취 검색: 연도와 키워드를 함께 걸면 둘 다 만족하는 것만 나온다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsFilteredByYearAndKeyword() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me")
                        .param("year", "2023")
                        .param("keyword", "정보처리"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("내 성취 검색: 검색어가 공백뿐이면 조건에서 빠진다")
    @WithUserDetails("user1@test.com")
    void getMyGoalsWithBlankKeyword() throws Exception {
        seedGoals();

        mvc.perform(get("/api/v1/goals/me").param("keyword", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @DisplayName("내 성취 검색: 남의 성취는 키워드가 맞아도 나오지 않는다")
    @WithUserDetails("user2@test.com")
    void getMyGoalsSearchExcludesOthers() throws Exception {
        saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        mvc.perform(get("/api/v1/goals/me").param("keyword", "정보처리"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    /* ---------- 개인 TODO 연결 ---------- */

    @Test
    @DisplayName("등록: 체크리스트에 개인 TODO 를 연결하면 detail.todoId 로 저장된다")
    @WithUserDetails("user1@test.com")
    void createChecklistWithTodo() throws Exception {
        PersonalTodo todo = saveTodoOf("user1@test.com", "기출 3회분 풀이");

        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "정보처리기사 취득", "todoId": %d }
                }
                """.formatted(todo.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.detail.todoId").value(todo.getId()));
    }

    @Test
    @DisplayName("등록: todoId 를 생략하면 연결 없이 등록된다")
    @WithUserDetails("user1@test.com")
    void createChecklistWithoutTodo() throws Exception {
        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "정보처리기사 취득" }
                }
                """)
                .andExpect(status().isCreated())
                // @JsonInclude(NON_NULL) 이라 연결이 없으면 필드 자체가 빠진다
                .andExpect(jsonPath("$.data.detail.todoId").doesNotExist());
    }

    @Test
    @DisplayName("등록: 남의 개인 TODO 는 연결할 수 없다 (403-1)")
    @WithUserDetails("user1@test.com")
    void cannotLinkOthersTodo() throws Exception {
        PersonalTodo others = saveTodoOf("user2@test.com", "항목1");

        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "정보처리기사 취득", "todoId": %d }
                }
                """.formatted(others.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("등록: 이미 다른 성취에 연결된 TODO 면 409-1 이다")
    @WithUserDetails("user1@test.com")
    void cannotLinkTodoTwice() throws Exception {
        PersonalTodo todo = saveTodoOf("user1@test.com", "항목1");
        String body = """
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "정보처리기사 취득", "todoId": %d }
                }
                """.formatted(todo.getId());

        createGoal(body).andExpect(status().isCreated());

        createGoal(body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("등록: 없는 TODO 를 연결하면 404-1 이다")
    @WithUserDetails("user1@test.com")
    void cannotLinkMissingTodo() throws Exception {
        createGoal("""
                {
                  "type": "CHECKLIST",
                  "status": "WANT",
                  "detail": { "title": "정보처리기사 취득", "todoId": 999999 }
                }
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("상세: 완료한 항목만 해낸 순서대로 온다 (미완료는 내보내지 않는다)")
    @WithUserDetails("user1@test.com")
    void detailCarriesOnlyDoneTodoItems() throws Exception {
        PersonalTodo todo = saveTodoOf("user1@test.com", "기출 3회분 풀이", "SQL 파트 정리", "모의고사 응시");
        List<PersonalTodoItem> items = personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, PageRequest.of(0, 10))
                .getContent();

        // 두 번째를 먼저 끝내고 첫 번째를 나중에 끝낸다 - 추가 순서가 아니라 완료 순서로 와야 한다
        items.get(1).complete();
        em.flush();
        items.get(0).complete();
        em.flush();

        long goalId = goalRepository.save(linkedChecklist("user1@test.com", todo)).getId();
        em.flush();
        em.clear();

        mvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todo.todoId").value(todo.getId()))
                .andExpect(jsonPath("$.data.todo.category").value("STUDY"))
                // 미완료 '모의고사 응시' 는 빠진다
                .andExpect(jsonPath("$.data.todo.items", hasSize(2)))
                .andExpect(jsonPath("$.data.todo.items[0].content").value("SQL 파트 정리"))
                .andExpect(jsonPath("$.data.todo.items[1].content").value("기출 3회분 풀이"))
                .andExpect(jsonPath("$.data.todo.items[*].done", everyItem(equalTo(true))))
                // 남은 개수를 유추할 수 있으면 감춘 게 아니다
                .andExpect(jsonPath("$.data.todo.totalCount").doesNotExist())
                .andExpect(jsonPath("$.data.todo.doneCount").doesNotExist());
    }

    @Test
    @DisplayName("상세: 완료한 항목이 하나도 없으면 items 가 빈 배열이다")
    @WithUserDetails("user1@test.com")
    void detailWithNoDoneItems() throws Exception {
        PersonalTodo todo = saveTodoOf("user1@test.com", "항목1", "항목2");
        long goalId = goalRepository.save(linkedChecklist("user1@test.com", todo)).getId();
        em.flush();
        em.clear();

        mvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todo.items", hasSize(0)));
    }

    @Test
    @DisplayName("상세: 연결이 없으면 todo 블록이 빠진다")
    @WithUserDetails("user1@test.com")
    void detailWithoutTodoBlock() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 취득");

        mvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todo").doesNotExist());
    }

    @Test
    @DisplayName("상세: 남의 성취에서도 TODO 항목이 보인다")
    @WithUserDetails("user1@test.com")
    void othersGoalStillCarriesTodoItems() throws Exception {
        PersonalTodo todo = saveTodoOf("user2@test.com", "항목1", "항목2");
        personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, PageRequest.of(0, 10))
                .getContent().get(0).complete();

        long goalId = goalRepository.save(linkedChecklist("user2@test.com", todo)).getId();
        em.flush();
        em.clear();

        mvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                // 완료한 한 건만 보인다
                .andExpect(jsonPath("$.data.todo.items", hasSize(1)));
    }

    @Test
    @DisplayName("수정: detail 에 todoId 를 빼면 연결이 끊긴다 (통째 교체 규칙)")
    @WithUserDetails("user1@test.com")
    void updateClearsTodoLink() throws Exception {
        PersonalTodo todo = saveTodoOf("user1@test.com", "항목1");
        long goalId = goalRepository.save(linkedChecklist("user1@test.com", todo)).getId();

        updateGoal(goalId, """
                { "detail": { "title": "정보처리기사 취득" } }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detail.todoId").doesNotExist());
    }

    @Test
    @DisplayName("수정: 이미 연결된 그 TODO 를 그대로 다시 보내도 통과한다")
    @WithUserDetails("user1@test.com")
    void updateKeepsSameTodoLink() throws Exception {
        PersonalTodo todo = saveTodoOf("user1@test.com", "항목1");
        long goalId = goalRepository.save(linkedChecklist("user1@test.com", todo)).getId();

        updateGoal(goalId, """
                { "detail": { "title": "정보처리기사 취득", "todoId": %d } }
                """.formatted(todo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detail.todoId").value(todo.getId()));
    }

    /** TODO 가 연결된 체크리스트 성취 하나 */
    private PersonalChecklist linkedChecklist(String email, PersonalTodo todo) {
        Member owner = memberRepository.findByEmail(email).orElseThrow();
        PersonalChecklist checklist =
                new PersonalChecklist(owner, GoalStatus.IN_PROGRESS, "정보처리기사 취득", null, null);
        checklist.linkTodo(todo);

        return checklist;
    }

    /* ---------- 성취 수정 ---------- */

    private ResultActions updateGoal(long goalId, String body) throws Exception {
        return mvc.perform(
                patch("/api/v1/goals/" + goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );
    }

    /** 수정 대상으로 쓸 자기신고 대회 성취를 하나 만들고 id 를 돌려준다 */
    private long saveContestGoalOf(String email) {
        Member owner = memberRepository.findByEmail(email).orElseThrow();
        return goalRepository.save(new PersonalContest(
                owner, GoalStatus.IN_PROGRESS, "사내 해커톤", true, null, null, null
        )).getId();
    }

    @Test
    @DisplayName("성취 수정: 상태와 내용을 함께 고친다")
    @WithUserDetails("user1@test.com")
    void updateGoalStatusAndDetail() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");

        updateGoal(goalId, """
                {
                  "status": "ACHIEVED",
                  "detail": {
                    "contestName": "2026 공공데이터 공모전",
                    "isTeam": true,
                    "result": "우수상",
                    "awardDate": "2026-08-24",
                    "contestUrl": "https://example.com/contest"
                  }
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("성취 수정 성공"))
                .andExpect(jsonPath("$.data.status").value("ACHIEVED"))
                .andExpect(jsonPath("$.data.type").value("CONTEST"))
                .andExpect(jsonPath("$.data.source").value("SELF_REPORTED"))
                .andExpect(jsonPath("$.data.detail.contestName").value("2026 공공데이터 공모전"))
                .andExpect(jsonPath("$.data.detail.result").value("우수상"))
                .andExpect(jsonPath("$.data.detail.contestUrl").value("https://example.com/contest"));
    }

    @Test
    @DisplayName("성취 수정: status 만 보내면 내용은 그대로 둔다")
    @WithUserDetails("user1@test.com")
    void updateGoalStatusOnly() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");

        updateGoal(goalId, """
                { "status": "ACHIEVED" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACHIEVED"))
                .andExpect(jsonPath("$.data.detail.contestName").value("사내 해커톤"));
    }

    /**
     * 수정 화면은 현재 상태도 선택지로 보여주므로, 상태를 그대로 둔 채 저장하는 요청이 실제로 들어온다.
     * 이걸 전이로 보면 409-2 로 튕겨 내용 수정 자체가 불가능해진다.
     */
    @Test
    @DisplayName("성취 수정: 현재와 같은 상태를 보내도 내용은 고쳐진다")
    @WithUserDetails("user1@test.com")
    void updateGoalWithSameStatus() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");

        updateGoal(goalId, """
                {
                  "status": "IN_PROGRESS",
                  "detail": { "contestName": "이름만 바꿈" }
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.detail.contestName").value("이름만 바꿈"));
    }

    /**
     * ACHIEVED 는 종료 상태라 더 전이할 수 없지만, 같은 상태로 두고 내용만 고치는 건 되어야 한다.
     */
    @Test
    @DisplayName("성취 수정: 이미 달성한 성취도 내용은 고칠 수 있다")
    @WithUserDetails("user1@test.com")
    void updateAchievedGoalDetail() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "토익 900점");
        updateGoal(goalId, """
                { "status": "ACHIEVED", "detail": { "title": "토익 900점" } }
                """).andExpect(status().isOk());

        updateGoal(goalId, """
                { "status": "ACHIEVED", "detail": { "title": "토익 950점", "memo": "재응시" } }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detail.title").value("토익 950점"))
                .andExpect(jsonPath("$.data.detail.memo").value("재응시"));
    }

    /**
     * detail 은 그 타입의 세부 정보를 통째로 교체한다.
     * 화면이 폼 전체를 보내오므로, 빠진 항목은 유지가 아니라 비우기다.
     */
    @Test
    @DisplayName("성취 수정: detail 에서 빠진 항목은 비워진다")
    @WithUserDetails("user1@test.com")
    void updateGoalDetailReplacesOmittedFields() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");
        updateGoal(goalId, """
                { "detail": { "contestName": "사내 해커톤", "result": "장려상", "contestUrl": "https://a.example" } }
                """).andExpect(status().isOk());

        updateGoal(goalId, """
                { "detail": { "contestName": "사내 해커톤" } }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detail.result").doesNotExist())
                .andExpect(jsonPath("$.data.detail.contestUrl").doesNotExist());
    }

    @Test
    @DisplayName("성취 수정: 증빙 파일 메타데이터를 기록한다")
    @WithUserDetails("user1@test.com")
    void updateGoalEvidenceMetadata() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");

        updateGoal(goalId, """
                {
                  "detail": {
                    "contestName": "사내 해커톤",
                    "evidenceFileName": "수상확인서.pdf",
                    "evidenceMimeType": "application/pdf",
                    "evidenceSize": 204800
                  }
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detail.evidenceFileName").value("수상확인서.pdf"))
                .andExpect(jsonPath("$.data.detail.evidenceMimeType").value("application/pdf"))
                .andExpect(jsonPath("$.data.detail.evidenceSize").value(204800));
    }

    /** 응답의 modifyAt 은 이번 수정이 반영된 값이어야 한다 (등록 시각과 달라야 한다) */
    @Test
    @DisplayName("성취 수정: 응답의 modifyDate 가 갱신된다")
    @WithUserDetails("user1@test.com")
    void updateGoalRefreshesModifyDate() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");

        updateGoal(goalId, """
                { "detail": { "contestName": "이름 변경" } }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modifyDate").value(
                        org.hamcrest.Matchers.not(equalTo(
                                goalRepository.findById(goalId).orElseThrow().getCreateDate().toString()))));
    }

    @Test
    @DisplayName("성취 수정: ACHIEVED 에서 IN_PROGRESS 로 되돌리면 409-2 다")
    @WithUserDetails("user1@test.com")
    void updateGoalWithBackwardTransition() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");
        updateGoal(goalId, """
                { "status": "ACHIEVED" }
                """).andExpect(status().isOk());

        updateGoal(goalId, """
                { "status": "IN_PROGRESS" }
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-2"));
    }

    @Test
    @DisplayName("성취 수정: 자동기록된 성취는 409-1 로 막힌다")
    @WithUserDetails("user1@test.com")
    void updateProjectGoalIsRejected() throws Exception {
        long goalId = savePartyAndProjectGoal("user1@test.com").getId();

        updateGoal(goalId, """
                { "status": "ACHIEVED" }
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("성취 수정: 남의 성취는 403-1 이다")
    @WithUserDetails("user2@test.com")
    void updateOthersGoalIsRejected() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        updateGoal(goalId, """
                { "status": "ACHIEVED" }
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("성취 수정: 타입별 필수 항목이 비면 400-4 다")
    @WithUserDetails("user1@test.com")
    void updateGoalWithoutRequiredField() throws Exception {
        long goalId = saveContestGoalOf("user1@test.com");

        updateGoal(goalId, """
                { "detail": { "result": "우수상" } }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    @DisplayName("성취 수정: 없는 성취는 404-1 이다")
    @WithUserDetails("user1@test.com")
    void updateGoalNotFound() throws Exception {
        updateGoal(999999L, """
                { "status": "ACHIEVED" }
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 수정: 비로그인이면 401-1 이다")
    void updateGoalWithoutLogin() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        updateGoal(goalId, """
                { "status": "ACHIEVED" }
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    /* ---------- 성취 삭제 ---------- */

    @Test
    @DisplayName("성취 삭제: 자기신고 성취를 지우면 204-1 이고 목록에서 사라진다")
    @WithUserDetails("user1@test.com")
    void deleteGoal() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        mvc.perform(delete("/api/v1/goals/" + goalId))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"))
                .andExpect(jsonPath("$.msg").value("성취 삭제 성공"));

        mvc.perform(get("/api/v1/goals/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("성취 삭제: 자동기록된 성취는 409-1 로 막힌다")
    @WithUserDetails("user1@test.com")
    void deleteProjectGoalIsRejected() throws Exception {
        long goalId = savePartyAndProjectGoal("user1@test.com").getId();

        mvc.perform(delete("/api/v1/goals/" + goalId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    @Test
    @DisplayName("성취 삭제: 남의 성취는 403-1 이다")
    @WithUserDetails("user2@test.com")
    void deleteOthersGoalIsRejected() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        mvc.perform(delete("/api/v1/goals/" + goalId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("성취 삭제: 없는 성취는 404-1 이다")
    @WithUserDetails("user1@test.com")
    void deleteGoalNotFound() throws Exception {
        mvc.perform(delete("/api/v1/goals/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("성취 삭제: 비로그인이면 401-1 이다")
    void deleteGoalWithoutLogin() throws Exception {
        long goalId = saveChecklistGoalOf("user1@test.com", "정보처리기사 실기");

        mvc.perform(delete("/api/v1/goals/" + goalId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
