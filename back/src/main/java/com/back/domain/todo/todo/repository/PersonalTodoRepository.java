package com.back.domain.todo.todo.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonalTodoRepository extends JpaRepository<PersonalTodo, Long> {

    /**
     * 내 TODO 검색. 성취 연혁과 달리 페이징한다 - 마이페이지 TODO 표가 이미 끊어 보여준다.
     *
     * status·linked 는 선택이라 null 이면 조건에서 빠진다.
     * linkedIds 는 성취에 연결된 TODO 들의 id 다. 연결 여부가 TODO 에 저장돼 있지 않아
     * (성취가 FK 를 갖는다) 호출자가 뽑아서 넘긴다.
     *
     * linkedIds 에 빈 목록을 넘기면 안 된다 - `in ()` 은 DB 에 따라 문법 오류다.
     * 비었을 때는 어떤 id 와도 맞지 않는 값 하나를 넣어 보낸다.
     */
    @Query("""
        select t from PersonalTodo t
        where t.owner = :owner
          and (:status is null or t.status = :status)
          and (:linked is null
               or (:linked = true and t.id in :linkedIds)
               or (:linked = false and t.id not in :linkedIds))
        order by t.createDate desc, t.id desc
        """)
    Page<PersonalTodo> searchMyTodos(
            @Param("owner") Member owner,
            @Param("status") TodoStatus status,
            @Param("linked") Boolean linked,
            @Param("linkedIds") List<Long> linkedIds,
            Pageable pageable
    );
}
