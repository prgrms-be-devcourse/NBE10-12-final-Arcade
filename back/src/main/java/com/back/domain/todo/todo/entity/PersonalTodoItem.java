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

    /**
     * 완료 처리. 이미 완료된 항목이면 아무 것도 하지 않는다.
     *
     * 화면이 완료 버튼을 누르는 순간 낙관적으로 상태를 바꾸고 요청을 보내므로 같은 요청이 두 번 올 수 있다.
     * 그때 doneAt 을 다시 찍으면 "6월에 끝낸 일"의 완료 시각이 오늘로 밀린다.
     */
    public void complete() {
        if (this.done) return;

        this.done = true;
        this.doneAt = LocalDateTime.now();
    }

    /**
     * 완료 취소. 지금 화면에는 이 경로가 없다 - 완료한 항목은 수정·삭제 버튼까지 사라진다.
     * 되돌리기를 열지는 화면 쪽 결정이라, 엔티티에는 자리만 만들어 두고 API 는 2차에서 붙인다.
     */
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
