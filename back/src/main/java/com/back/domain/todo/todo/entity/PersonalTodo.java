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

    /**
     * 상태를 인자로 받지 않고 WANT 로 시작한다. 새로 만든 TODO 는 아직 손대기 전이고,
     * 만들자마자 HOLD 나 ACHIEVED 인 TODO 는 성립하지 않는다.
     * (Party 가 RECRUITING, Project 가 IN_PROGRESS 를 생성자에서 고정하는 것과 같다.)
     *
     * 프론트 등록 화면도 상태를 보내지 않는다 - createTodo 가 title·category·memo 만 넘긴다.
     */
    public PersonalTodo(Member owner, String title, TodoCategory category, String memo) {
        this.owner = owner;
        this.title = title;
        this.category = category;
        this.memo = memo;
        this.status = TodoStatus.WANT;
    }

    public void update(String title, TodoCategory category, String memo) {
        this.title = title;
        this.category = category;
        this.memo = memo;
    }

    /**
     * 상태는 자유롭게 오간다. 완료한 할 일을 다시 여는 건 정상적인 사용이라 전이 규칙을 두지 않는다.
     * 성취(GoalStatus)와 다른 점이고, TodoStatus 를 따로 둔 이유이기도 하다.
     *
     * 항목을 전부 체크했다고 해서 자동으로 ACHIEVED 가 되지는 않는다 - 사용자가 걸어둔 HOLD 를 덮어쓰게 된다.
     * 화면의 '목록 완료 처리' 버튼이 이 메서드를 부른다.
     */
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
}
