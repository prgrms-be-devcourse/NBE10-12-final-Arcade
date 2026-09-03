package com.back.domain.todo.todo.repository;

import com.back.domain.todo.todo.dtos.TodoProgressDto;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.PersonalTodoItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 항목의 유일한 접근 경로. PersonalTodo 가 컬렉션을 들지 않아 저장·조회·삭제가 전부 여기를 지난다. */
public interface PersonalTodoItemRepository extends JpaRepository<PersonalTodoItem, Long> {

    Page<PersonalTodoItem> findAllByTodoOrderBySortOrderAscIdAsc(PersonalTodo todo, Pageable pageable);

    // 진행률은 페이지가 아니라 전체 기준이라 목록 응답만으로는 셀 수 없다.
    long countByTodo(PersonalTodo todo);

    long countByTodoAndDoneIsTrue(PersonalTodo todo);

    /**
     * 목록 화면의 진행률. TODO 마다 카운트 2번을 날리면 한 페이지(20건)에 41쿼리가 된다.
     * 항목이 없는 TODO 는 결과에 없으니 0 으로 채워야 한다.
     */
    @Query("""
        select new com.back.domain.todo.todo.dtos.TodoProgressDto(
            i.todo.id, count(i), sum(case when i.done = true then 1L else 0L end))
        from PersonalTodoItem i
        where i.todo.id in :todoIds
        group by i.todo.id
        """)
    List<TodoProgressDto> findProgressByTodoIds(@Param("todoIds") List<Long> todoIds);

    // 새 항목의 표시 순서. 항목이 없으면 0.
    @Query("select coalesce(max(i.sortOrder), -1) + 1 from PersonalTodoItem i where i.todo = :todo")
    int nextSortOrder(@Param("todo") PersonalTodo todo);

    /**
     * TODO 삭제 전 항목 정리. cascade 가 없어 남은 항목이 FK 로 삭제를 막는다.
     * 벌크(@Modifying)로 두면 지워진 항목이 영속성 컨텍스트에 남아 flush 에서 터진다.
     */
    void deleteAllByTodo(PersonalTodo todo);
}
