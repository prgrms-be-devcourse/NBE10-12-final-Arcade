package com.back.domain.todo.todo.controller;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.PersonalTodoItem;
import com.back.domain.todo.todo.entity.TodoCategory;
import com.back.domain.todo.todo.repository.PersonalTodoItemRepository;
import com.back.domain.todo.todo.repository.PersonalTodoRepository;
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

import static org.hamcrest.Matchers.hasSize;
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
public class ApiV1PersonalTodoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PersonalTodoRepository personalTodoRepository;

    @Autowired
    private PersonalTodoItemRepository personalTodoItemRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member(String email) {
        return memberRepository.findByEmail(email).orElseThrow();
    }

    private PersonalTodo saveTodo(String email) {
        return personalTodoRepository.save(new PersonalTodo(
                member(email), "정보처리기사 실기 준비", TodoCategory.STUDY, "매주 토요일 2시간"
        ));
    }

    private PersonalTodoItem saveItem(PersonalTodo todo, String content) {
        return personalTodoItemRepository.save(new PersonalTodoItem(
                todo, content, personalTodoItemRepository.nextSortOrder(todo)
        ));
    }

    private ResultActions createTodo(String body) throws Exception {
        return mvc.perform(post("/api/v1/todos").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /* ---------- 등록 ---------- */

    @Test
    @DisplayName("등록: WANT 로 만들어지고 진행률은 0 이다")
    @WithUserDetails("user1@test.com")
    void create() throws Exception {
        createTodo("""
                { "title": "정보처리기사 실기 준비", "category": "STUDY", "memo": "매주 토요일 2시간" }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.status").value("WANT"))
                .andExpect(jsonPath("$.data.category").value("STUDY"))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.doneCount").value(0));
    }

    @Test
    @DisplayName("등록: 제목이 없으면 400-1 이다")
    @WithUserDetails("user1@test.com")
    void createWithoutTitle() throws Exception {
        createTodo("""
                { "category": "STUDY" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("등록: 정의되지 않은 분류면 400-2 이다 (본문 파싱 실패)")
    @WithUserDetails("user1@test.com")
    void createWithUnknownCategory() throws Exception {
        createTodo("""
                { "title": "제목", "category": "루틴" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @DisplayName("등록: 미로그인이면 401 이다")
    void createWithoutLogin() throws Exception {
        createTodo("""
                { "title": "제목", "category": "STUDY" }
                """)
                .andExpect(status().isUnauthorized());
    }

    /* ---------- 목록 ---------- */

    @Test
    @DisplayName("목록: 내 TODO 만 나오고 진행률이 함께 온다")
    @WithUserDetails("user1@test.com")
    void getMyTodos() throws Exception {
        PersonalTodo mine = saveTodo("user1@test.com");
        saveItem(mine, "항목1");
        saveItem(mine, "항목2").complete();
        saveTodo("user2@test.com");

        mvc.perform(get("/api/v1/todos/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].totalCount").value(2))
                .andExpect(jsonPath("$.data.content[0].doneCount").value(1));
    }

    @Test
    @DisplayName("목록: 항목이 없는 TODO 는 진행률이 0 이다")
    @WithUserDetails("user1@test.com")
    void getMyTodosWithoutItems() throws Exception {
        saveTodo("user1@test.com");

        mvc.perform(get("/api/v1/todos/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].totalCount").value(0));
    }

    @Test
    @DisplayName("목록: status 로 거를 수 있다")
    @WithUserDetails("user1@test.com")
    void getMyTodosFilteredByStatus() throws Exception {
        saveTodo("user1@test.com");

        mvc.perform(get("/api/v1/todos/me").param("status", "ACHIEVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    /* ---------- 상세 ---------- */

    @Test
    @DisplayName("상세: 항목 첫 페이지와 진행률이 함께 온다")
    @WithUserDetails("user1@test.com")
    void getTodo() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");
        saveItem(todo, "기출 3회분 풀이");
        saveItem(todo, "SQL 파트 정리");

        mvc.perform(get("/api/v1/todos/" + todo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].content").value("기출 3회분 풀이"))
                .andExpect(jsonPath("$.data.hasMoreItems").value(false))
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    @DisplayName("상세: 남의 TODO 는 403-1 이다")
    @WithUserDetails("user1@test.com")
    void getOthersTodo() throws Exception {
        PersonalTodo others = saveTodo("user2@test.com");

        mvc.perform(get("/api/v1/todos/" + others.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("상세: 없는 TODO 는 404-1 이다")
    @WithUserDetails("user1@test.com")
    void getMissingTodo() throws Exception {
        mvc.perform(get("/api/v1/todos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    /* ---------- 수정 ---------- */

    @Test
    @DisplayName("수정: 넘긴 필드만 바뀌고 나머지는 유지된다")
    @WithUserDetails("user1@test.com")
    void updatePartially() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");

        mvc.perform(patch("/api/v1/todos/" + todo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "memo": "실기 2026.10.18", "status": "IN_PROGRESS" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memo").value("실기 2026.10.18"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                // 넘기지 않은 제목·분류는 그대로다
                .andExpect(jsonPath("$.data.title").value("정보처리기사 실기 준비"))
                .andExpect(jsonPath("$.data.category").value("STUDY"));
    }

    @Test
    @DisplayName("수정: 제목을 빈 값으로 보내면 400-4 이다")
    @WithUserDetails("user1@test.com")
    void updateWithBlankTitle() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");

        mvc.perform(patch("/api/v1/todos/" + todo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    @DisplayName("수정: 완료한 TODO 도 다시 열 수 있다 (전이 제약 없음)")
    @WithUserDetails("user1@test.com")
    void reopenAchievedTodo() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");

        mvc.perform(patch("/api/v1/todos/" + todo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "ACHIEVED" }
                                """))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/todos/" + todo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "IN_PROGRESS" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    /* ---------- 삭제 ---------- */

    @Test
    @DisplayName("삭제: 항목까지 함께 지워진다")
    @WithUserDetails("user1@test.com")
    void deleteTodo() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");
        saveItem(todo, "항목1");

        mvc.perform(delete("/api/v1/todos/" + todo.getId()))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"));

        mvc.perform(get("/api/v1/todos/" + todo.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("삭제: 진행 중인 성취에 연결돼 있으면 연결만 끊고 지워진다")
    @WithUserDetails("user1@test.com")
    void deleteTodoLinkedToUnfinishedGoal() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");
        saveItem(todo, "항목1");

        PersonalChecklist goal = new PersonalChecklist(
                member("user1@test.com"), GoalStatus.IN_PROGRESS, "정보처리기사 실기 준비", null, null
        );
        goal.linkTodo(todo);
        goalRepository.save(goal);

        mvc.perform(delete("/api/v1/todos/" + todo.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/goals/" + goal.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("삭제: 완료된 성취에 연결돼 있으면 409-1 이다")
    @WithUserDetails("user1@test.com")
    void deleteTodoLinkedToAchievedGoal() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");

        PersonalChecklist goal = new PersonalChecklist(
                member("user1@test.com"), GoalStatus.ACHIEVED, "정보처리기사 취득", null, null
        );
        goal.linkTodo(todo);
        goalRepository.save(goal);

        mvc.perform(delete("/api/v1/todos/" + todo.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"));
    }

    /* ---------- 할 일 항목 ---------- */

    @Test
    @DisplayName("항목 추가: 맨 뒤에 붙는다")
    @WithUserDetails("user1@test.com")
    void addItem() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");
        saveItem(todo, "항목1");

        mvc.perform(post("/api/v1/todos/" + todo.getId() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "항목2" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.content").value("항목2"))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.done").value(false));
    }

    @Test
    @DisplayName("항목 추가: 내용이 비면 400-1 이다")
    @WithUserDetails("user1@test.com")
    void addBlankItem() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");

        mvc.perform(post("/api/v1/todos/" + todo.getId() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("항목 완료: doneAt 이 찍히고 다시 불러도 그대로다")
    @WithUserDetails("user1@test.com")
    void completeItem() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");
        PersonalTodoItem item = saveItem(todo, "항목1");
        String url = "/api/v1/todos/" + todo.getId() + "/items/" + item.getId() + "/complete";

        String first = mvc.perform(post(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.done").value(true))
                .andReturn().getResponse().getContentAsString();

        String doneAt = com.jayway.jsonpath.JsonPath.read(first, "$.data.doneAt");

        mvc.perform(post(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.doneAt").value(doneAt));
    }

    @Test
    @DisplayName("항목 수정·삭제: 다른 TODO 의 항목이면 404-1 이다")
    @WithUserDetails("user1@test.com")
    void itemOfAnotherTodo() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");
        PersonalTodo other = saveTodo("user1@test.com");
        PersonalTodoItem item = saveItem(other, "남의 목록 항목");

        mvc.perform(delete("/api/v1/todos/" + todo.getId() + "/items/" + item.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("항목 목록: 페이징된다")
    @WithUserDetails("user1@test.com")
    void getItemsPaged() throws Exception {
        PersonalTodo todo = saveTodo("user1@test.com");
        for (int i = 1; i <= 5; i++) saveItem(todo, "항목" + i);

        mvc.perform(get("/api/v1/todos/" + todo.getId() + "/items")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(5));
    }
}
