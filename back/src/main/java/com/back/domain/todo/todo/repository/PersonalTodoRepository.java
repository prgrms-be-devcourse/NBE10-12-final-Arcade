package com.back.domain.todo.todo.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalTodoRepository extends JpaRepository<PersonalTodo, Long> {

    // 성취 연혁과 달리 페이징한다. 마이페이지 TODO 표가 이미 끊어 보여준다.
    Page<PersonalTodo> findAllByOwnerOrderByCreateDateDesc(Member owner, Pageable pageable);

    Page<PersonalTodo> findAllByOwnerAndStatusOrderByCreateDateDesc(
            Member owner,
            TodoStatus status,
            Pageable pageable
    );
}
