package com.back.domain.todo.todo.entity;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.todo.todo.repository.PersonalTodoItemRepository;
import com.back.domain.todo.todo.repository.PersonalTodoRepository;
import com.back.global.exception.ServiceException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 개인 TODO 뼈대 검증 (작업표 arc-84).
 *
 * 스키마가 실제로 서는지(FK·UNIQUE)와 항목 연산·삭제 규칙을 고정한다.
 * API는 아직 없고 엔티티·리포지토리까지가 이번 범위다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class PersonalTodoTest {

    @Autowired
    private PersonalTodoRepository personalTodoRepository;

    @Autowired
    private PersonalTodoItemRepository personalTodoItemRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    private Member user1() {
        return memberRepository.findByEmail("user1@test.com").orElseThrow();
    }

    private PersonalTodo saveTodo() {
        return personalTodoRepository.save(new PersonalTodo(
                user1(), "정보처리기사 실기 준비", TodoCategory.STUDY, "매주 토요일 2시간"
        ));
    }

    /** 화면과 같은 흐름 - TODO를 먼저 만들고 상세에서 항목을 한 건씩 추가한다 */
    private PersonalTodo saveTodoWith(String... contents) {
        PersonalTodo todo = saveTodo();

        for (String content : contents) {
            personalTodoItemRepository.save(new PersonalTodoItem(
                    todo, content, personalTodoItemRepository.nextSortOrder(todo)
            ));
        }

        return todo;
    }

    @Test
    @DisplayName("개인 TODO: 항목 없이 WANT 로 생성된다")
    void createsEmptyTodoAsWant() {
        PersonalTodo saved = saveTodo();
        em.flush();
        em.clear();

        PersonalTodo found = personalTodoRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getCategory()).isEqualTo(TodoCategory.STUDY);
        // 새로 만든 TODO 는 아직 손대기 전이라 WANT 로 시작한다
        assertThat(found.getStatus()).isEqualTo(TodoStatus.WANT);
        assertThat(personalTodoItemRepository.countByTodo(found)).isZero();
    }

    @Test
    @DisplayName("항목 추가: 추가한 순서대로 sortOrder 가 매겨진다")
    void addsItemsInOrder() {
        PersonalTodo todo = saveTodoWith("기출 3회분 풀이", "SQL 파트 정리", "모의고사 응시");
        em.flush();

        Page<PersonalTodoItem> items = personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, PageRequest.of(0, 10));

        assertThat(items.getContent())
                .extracting(PersonalTodoItem::getContent)
                .containsExactly("기출 3회분 풀이", "SQL 파트 정리", "모의고사 응시");
        assertThat(items.getContent())
                .extracting(PersonalTodoItem::getSortOrder)
                .containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("항목 추가: 빈 내용은 거부한다 (400-4)")
    void rejectsBlankContent() {
        PersonalTodo todo = saveTodo();

        assertThatThrownBy(() -> new PersonalTodoItem(todo, "   ", 0))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("할 일 내용");
    }

    @Test
    @DisplayName("항목 완료: 완료 시각이 찍히고, 다시 완료해도 처음 시각이 유지된다")
    void completeKeepsFirstDoneAt() {
        PersonalTodo todo = saveTodoWith("기출 3회분 풀이");
        PersonalTodoItem item = personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, PageRequest.of(0, 10))
                .getContent().get(0);

        item.complete();
        em.flush();

        var doneAtBefore = item.getDoneAt();
        assertThat(doneAtBefore).isNotNull();

        // 화면이 낙관적으로 상태를 바꾸고 요청을 보내서 같은 완료 요청이 두 번 올 수 있다
        item.complete();
        em.flush();

        assertThat(item.getDoneAt()).isEqualTo(doneAtBefore);
    }

    @Test
    @DisplayName("항목 수정·삭제: 한 건씩 처리되고 진행률 카운트는 전체 기준이다")
    void updatesAndRemovesSingleItem() {
        PersonalTodo todo = saveTodoWith("항목1", "항목2", "항목3");
        var items = personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, PageRequest.of(0, 10))
                .getContent();

        items.get(0).updateContent("항목1 수정");
        items.get(0).complete();
        personalTodoItemRepository.delete(items.get(2));
        em.flush();
        em.clear();

        PersonalTodo found = personalTodoRepository.findById(todo.getId()).orElseThrow();

        assertThat(personalTodoItemRepository.countByTodo(found)).isEqualTo(2);
        assertThat(personalTodoItemRepository.countByTodoAndDoneIsTrue(found)).isEqualTo(1);
        assertThat(personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(found, PageRequest.of(0, 10))
                .getContent())
                .extracting(PersonalTodoItem::getContent)
                .containsExactly("항목1 수정", "항목2");
    }

    @Test
    @DisplayName("항목 조회: 페이징된다")
    void findsItemsWithPaging() {
        PersonalTodo todo = saveTodoWith("항목1", "항목2", "항목3", "항목4", "항목5");
        em.flush();

        Page<PersonalTodoItem> firstPage = personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, PageRequest.of(0, 2));

        assertThat(firstPage.getContent())
                .extracting(PersonalTodoItem::getContent)
                .containsExactly("항목1", "항목2");
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("TODO 삭제: 항목을 먼저 지워야 한다 (cascade 가 없다)")
    void deletingTodoRequiresRemovingItemsFirst() {
        PersonalTodo todo = saveTodoWith("항목1", "항목2");
        em.flush();

        personalTodoItemRepository.deleteAllByTodo(todo);
        personalTodoRepository.delete(todo);
        em.flush();
        em.clear();

        assertThat(personalTodoRepository.findById(todo.getId())).isEmpty();
    }

    @Test
    @DisplayName("성취 연결: TODO 하나는 성취 한 건에만 연결된다 (personal_todo_id UNIQUE)")
    void todoCanBeLinkedToOnlyOneGoal() {
        PersonalTodo todo = saveTodoWith("항목1");
        Member owner = user1();

        PersonalChecklist first = new PersonalChecklist(
                owner, GoalStatus.ACHIEVED, "정보처리기사 실기 준비", null, LocalDate.of(2026, 10, 18)
        );
        first.linkTodo(todo);
        goalRepository.save(first);
        em.flush();

        PersonalChecklist duplicate = new PersonalChecklist(
                owner, GoalStatus.ACHIEVED, "같은 TODO 를 또 등록", null, null
        );
        duplicate.linkTodo(todo);

        // JOINED 상속 + IDENTITY 채번이라 save() 가 그 자리에서 INSERT 를 날린다.
        // 그래서 제약 위반이 flush() 가 아니라 save() 에서 터진다.
        assertThatThrownBy(() -> {
            goalRepository.save(duplicate);
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("TODO 삭제: 진행 중인 성취에 연결돼 있으면 성취의 FK 를 끊고 지울 수 있다")
    void deletingTodoDetachesLinkFromUnfinishedGoal() {
        PersonalTodo todo = saveTodoWith("항목1", "항목2");

        PersonalChecklist goal = new PersonalChecklist(
                user1(), GoalStatus.IN_PROGRESS, "정보처리기사 실기 준비", null, null
        );
        goal.linkTodo(todo);
        goalRepository.save(goal);
        em.flush();

        PersonalChecklist linked = goalRepository.findChecklistByPersonalTodoId(todo.getId()).orElseThrow();
        linked.detachTodo();

        // FK 를 끊는 UPDATE 가 TODO 의 DELETE 보다 먼저 나가야 한다. 이 flush 를 빼면 제약에 걸린다.
        goalRepository.flush();
        personalTodoItemRepository.deleteAllByTodo(todo);
        personalTodoRepository.delete(todo);
        em.flush();
        em.clear();

        assertThat(personalTodoRepository.findById(todo.getId())).isEmpty();

        // 성취는 남고 연결만 끊긴다 (발자취는 잃는다 - 화면이 미리 알려줘야 하는 지점)
        PersonalChecklist found = (PersonalChecklist) goalRepository.findById(goal.getId()).orElseThrow();
        assertThat(found.getPersonalTodo()).isNull();
    }

    @Test
    @DisplayName("TODO 삭제: 완료된 성취에 연결돼 있으면 끊을 수 없다 (409-1)")
    void cannotDetachTodoFromAchievedGoal() {
        PersonalTodo todo = saveTodoWith("항목1");

        PersonalChecklist achieved = new PersonalChecklist(
                user1(), GoalStatus.ACHIEVED, "정보처리기사 취득", null, null
        );
        achieved.linkTodo(todo);
        goalRepository.save(achieved);
        em.flush();

        PersonalChecklist linked = goalRepository.findChecklistByPersonalTodoId(todo.getId()).orElseThrow();

        assertThatThrownBy(linked::detachTodo)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("완료된 성취");

        assertThat(linked.getPersonalTodo()).isNotNull();
    }

    @Test
    @DisplayName("성취 삭제: 연결된 TODO 와 항목은 그대로 남는다")
    void deletingGoalKeepsTodo() {
        PersonalTodo todo = saveTodoWith("항목1", "항목2");

        PersonalChecklist goal = new PersonalChecklist(
                user1(), GoalStatus.ACHIEVED, "정보처리기사 취득", null, null
        );
        goal.linkTodo(todo);
        goalRepository.save(goal);
        em.flush();

        goalRepository.delete(goal);
        em.flush();
        em.clear();

        assertThat(goalRepository.findById(goal.getId())).isEmpty();

        PersonalTodo found = personalTodoRepository.findById(todo.getId()).orElseThrow();
        assertThat(personalTodoItemRepository.countByTodo(found)).isEqualTo(2);
    }

    @Test
    @DisplayName("성취 연결: 연결하지 않은 체크리스트 성취는 personalTodo 가 null 이다")
    void goalWithoutTodoKeepsNullLink() {
        PersonalChecklist standalone = new PersonalChecklist(
                user1(), GoalStatus.WANT, "직접 등록한 목표", "메모", null
        );
        goalRepository.save(standalone);
        em.flush();
        em.clear();

        PersonalChecklist found = (PersonalChecklist) goalRepository.findById(standalone.getId()).orElseThrow();

        assertThat(found.getPersonalTodo()).isNull();
    }
}
