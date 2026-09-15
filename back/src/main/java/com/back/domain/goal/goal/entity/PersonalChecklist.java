package com.back.domain.goal.goal.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.global.exception.ServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 자격/인증, 도전/경험, 자유 목표를 제목+메모 수준으로 단순화한 체크리스트형 목표.
// 완료 여부는 별도 필드 없이 Goal.status(ACHIEVED)로 표현한다(2.5).
@Entity
@Getter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "goal_id")
public class PersonalChecklist extends Goal {

    @Column(nullable = false)
    private String title;

    private String memo;

    // 시작일 또는 목표일. 상태에 따라 '2026.07 ~ 진행중', '2026.10 예정'처럼 표기된다(2.11).
    private LocalDate targetDate;

    /**
     * 이 성취가 어느 개인 TODO에서 나왔는지. 연결 없이 직접 등록한 성취는 null이다.
     *
     * sourcePartyId를 값만 들고 있는 것과 달리 여기는 FK로 잡았다.
     * 그래야 연결된 TODO가 DB 제약으로 삭제되지 않아, 성취 상세가 가리키는 진행 과정이 사라지지 않는다.
     * 파티는 성취보다 먼저 있고 양방향이 될 위험이 있어 피했지만, TODO는 성취가 단방향으로 가리키기만 한다.
     *
     * UNIQUE인 이유는 TODO 하나가 성취 여러 건으로 중복 등록되면
     * 같은 발자취가 연혁에 여러 번 쌓이고, '아직 성취로 등록되지 않은 TODO' 목록도 성립하지 않기 때문이다.
     * (Goal.party_assemble_to_member_id를 UNIQUE로 둔 것과 같은 사고다.)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_todo_id", unique = true)
    private PersonalTodo personalTodo;

    public PersonalChecklist(Member owner, GoalStatus status, String title, String memo, LocalDate targetDate) {
        super(owner, GoalType.CHECKLIST, status, GoalSource.SELF_REPORTED, null, null);
        this.title = title;
        this.memo = memo;
        this.targetDate = targetDate;
    }

    public void update(String title, String memo, LocalDate targetDate) {
        this.title = title;
        this.memo = memo;
        this.targetDate = targetDate;
    }

    // 등록 직후 서비스가 이어서 호출한다 (PersonalContest.updateEvidenceMetadata와 같은 자리).
    // 생성자에 넣지 않은 건 연결이 선택이고, 기존 등록 경로를 건드리지 않기 위해서다.
    public void linkTodo(PersonalTodo personalTodo) {
        this.personalTodo = personalTodo;
    }

    /**
     * 연결된 개인 TODO를 떼어낸다. TODO 삭제 요청이 들어왔을 때 TODO를 지우기 전에 먼저 호출한다.
     * FK가 성취 쪽에 있어서, 이걸 먼저 하지 않으면 TODO 삭제가 제약에 걸린다.
     *
     * 완료된 성취는 떼어낼 수 없다. ACHIEVED 성취의 진행 과정은 "이 사람이 실제로 무엇을 했는지"를
     * 받쳐주는 근거라, 지우면 성취만 남고 근거가 사라진다.
     * 아직 진행 중인 성취라면 떼어내도 된다 - 다만 그 성취는 발자취를 잃으므로 화면에서 미리 알려야 한다.
     *
     * 주의: 호출 후 TODO를 지우기 전에 flush가 필요하다.
     * 그러지 않으면 하이버네이트가 이 UPDATE보다 TODO의 DELETE를 먼저 내보내 FK 제약에 걸린다.
     */
    public void detachTodo() {
        if (getStatus() == GoalStatus.ACHIEVED) {
            throw new ServiceException("409-1", "완료된 성취에 연결된 개인 TODO는 삭제할 수 없습니다.");
        }

        this.personalTodo = null;
    }
}
