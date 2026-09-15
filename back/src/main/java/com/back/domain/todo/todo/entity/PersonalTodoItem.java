package com.back.domain.todo.todo.entity;

import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 개인 TODO 안의 작은 할 일
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "personal_todo_item",
        indexes = @Index(name = "idx_personal_todo_item_todo", columnList = "todo_id")
)
public class PersonalTodoItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false)
    private PersonalTodo todo;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean done;

    // 체크한 시점.
    private LocalDateTime doneAt;

    // 화면에 보이는 순서. 추가된 순서를 그대로 쓴다 - 화면에 순서를 바꾸는 UI 가 없다.
    @Column(nullable = false)
    private int sortOrder;

    public PersonalTodoItem(PersonalTodo todo, String content, int sortOrder) {
        this.todo = todo;
        this.content = requireContent(content);
        this.sortOrder = sortOrder;
        this.done = false;
    }

    public void updateContent(String content) {
        this.content = requireContent(content);
    }

    /** 멱등. 화면이 낙관적으로 갱신해 같은 요청이 두 번 올 수 있는데, doneAt 이 오늘로 밀리면 안 된다. */
    public void complete() {
        if (this.done) return;

        this.done = true;
        this.doneAt = LocalDateTime.now();
    }

    /** 완료 취소. 화면에 되돌리기 UI 가 없어 API 는 열지 않았다. */
    public void reopen() {
        if (!this.done) return;

        this.done = false;
        this.doneAt = null;
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ServiceException("400-4", "할 일 내용을 입력해주세요.");
        }

        return content;
    }
}
