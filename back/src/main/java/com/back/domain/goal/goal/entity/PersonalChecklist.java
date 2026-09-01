package com.back.domain.goal.goal.entity;

import com.back.domain.member.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}
