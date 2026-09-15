package com.back.domain.todo.todo.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface PersonalTodoRepository extends JpaRepository<PersonalTodo, Long> {

    /**
     * 내 TODO 검색. 성취 연혁과 달리 페이징한다 - 마이페이지 TODO 표가 이미 끊어 보여준다.
     *
     * status·linked 는 선택이라 null 이면 조건에서 빠진다.
     *
     * 연결 여부는 성취(PersonalChecklist)가 FK 로 갖고 TODO 는 모른다.
     * 그래서 연결된 id 를 메모리로 가져와 in 절에 넘기는 대신 exists 서브쿼리로 DB 에서 거른다 -
     * 회원의 성취가 늘어도 목록이 통째로 올라오지 않고, in 절 파라미터 개수 한도에도 걸리지 않는다.
     * (todo 쿼리가 성취 엔티티를 참조하는데, PersonalTodoService 가 GoalRepository 를 직접 쓰는 것과 같은 방향이다.)
     */
    @Query("""
        select t from PersonalTodo t
        where t.owner = :owner
          and (:status is null or t.status = :status)
          and (:linked is null
               or (:linked = true
                   and exists (select 1 from PersonalChecklist c where c.personalTodo = t))
               or (:linked = false
                   and not exists (select 1 from PersonalChecklist c where c.personalTodo = t)))
        order by t.createDate desc, t.id desc
        """)
    Page<PersonalTodo> searchMyTodos(
            @Param("owner") Member owner,
            @Param("status") TodoStatus status,
            @Param("linked") Boolean linked,
            Pageable pageable
    );
}
