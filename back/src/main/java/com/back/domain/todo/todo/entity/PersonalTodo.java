package com.back.domain.todo.todo.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개인 TODO - 큰 주제 하나와 그 아래 작은 할 일들.
 *
 * 성취(Goal)와는 독립이다. 성취로 남길 생각이 없는 잡일도 여기 담을 수 있어야 해서 별도 도메인으로 뒀다.
 * 나중에 이력으로 남기고 싶어지면 CHECKLIST 성취(PersonalChecklist)가 이 TODO 를 FK 로 가리키고,
 * 성취 상세가 여기 항목들을 읽어 진행 과정을 보여준다. 연결은 선택이고, 방향은 성취 -> TODO 단방향이다.

 */
@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "personal_todo",
        indexes = @Index(name = "idx_personal_todo_owner", columnList = "owner_id")
)
public class PersonalTodo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TodoCategory category;

    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TodoStatus status;

    /** 등록 화면이 상태를 보내지 않는다. 새 TODO 는 손대기 전이라 WANT 로 시작한다. */
    public PersonalTodo(Member owner, String title, TodoCategory category, String memo) {
        this.owner = owner;
        this.title = requireTitle(title);
        this.category = category;
        this.memo = memo;
        this.status = TodoStatus.WANT;
    }

    /** 부분 수정. null 은 그대로 두고, 빈 문자열은 비운다. 화면이 메모를 따로 저장한다. */
    public void update(String title, TodoCategory category, String memo) {
        if (title != null) this.title = requireTitle(title);
        if (category != null) this.category = category;
        if (memo != null) this.memo = memo;
    }

    /** 전이 규칙 없음. 완료한 할 일을 다시 여는 건 정상이다. 항목을 다 체크해도 자동 전이하지 않는다. */
    public void changeStatus(TodoStatus next) {
        this.status = next;
    }

    public boolean isOwnedBy(Member member) {
        return this.owner.getId().equals(member.getId());
    }

    public void checkOwnedBy(Member member) {
        if (!isOwnedBy(member)) {
            throw new ServiceException("403-1", "본인의 개인 TODO만 조회/수정할 수 있습니다.");
        }
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ServiceException("400-4", "제목을 입력해주세요.");
        }

        return title;
    }
}
