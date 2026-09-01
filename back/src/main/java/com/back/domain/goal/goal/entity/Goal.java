package com.back.domain.goal.goal.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(
        name = "goal",
        // 같은 파티 확정 건으로 한 회원에게 Goal이 두 번 생기지 않게 막는다 (이벤트 중복 수신 방어).
        // 자기신고는 이 값이 null이라 제약에 걸리지 않는다.
        uniqueConstraints = @UniqueConstraint(columnNames = "party_assemble_to_member_id"),
        indexes = {
                @Index(name = "idx_goal_owner", columnList = "owner_id"),
                @Index(name = "idx_goal_source_party", columnList = "source_party_id")
        }
)
public abstract class Goal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private GoalType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private GoalSource source;

    // 파티 확정으로 자동 생성된 경우 그 원본 PARTY_ASSEMBLE_TO_MEMBER 행을 가리킨다.
    // Party 도메인과의 결합을 피하려고 연관관계 없이 값만 보관한다 (Party.targetContestId와 같은 방식).
    //
    // 지금은 위 UK로 중복 생성을 막는 용도로만 쓰고 이 값을 타고 조회하는 코드는 없다.
    // (owner_id, source_party_id, type) 조합이 같은 행을 지목하므로 그쪽 UK로 대체할 수도 있지만,
    // 확정 사건 원본으로 역추적하는 경로를 남겨두려고 유지한다. 끝까지 쓰이지 않으면 그때 제거한다.
    @Column(name = "party_assemble_to_member_id")
    private Long partyAssembleToMemberId;

    // 좋아요·조회수 집계 대상이 되는 파티. PLATFORM_VERIFIED일 때만 값이 있다(3.2).
    @Column(name = "source_party_id")
    private Long sourcePartyId;

    // 상세 조회 시마다 증가하는 단순 카운트.
    // PLATFORM_VERIFIED는 sourcePartyId가 가리키는 PARTY.viewCount에 합산되므로 여기엔 쌓이지 않는다(3.2).
    @Column(nullable = false)
    private int viewCount;

    protected Goal(
            Member owner,
            GoalType type,
            GoalStatus status,
            GoalSource source,
            Long partyAssembleToMemberId,
            Long sourcePartyId
    ) {
        this.owner = owner;
        this.type = type;
        this.status = status;
        this.source = source;
        this.partyAssembleToMemberId = partyAssembleToMemberId;
        this.sourcePartyId = sourcePartyId;
        this.viewCount = 0;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public boolean isOwnedBy(Member member) {
        return this.owner.getId().equals(member.getId());
    }

    public boolean isPlatformVerified() {
        return this.source == GoalSource.PLATFORM_VERIFIED;
    }

    public void checkOwnedBy(Member member) {
        if (!isOwnedBy(member)) {
            throw new ServiceException("403-1", "본인 소유의 성취만 수정/삭제할 수 있습니다.");
        }
    }

    public void checkModifiable() {
        if (isPlatformVerified()) {
            throw new ServiceException("409-1", "플랫폼에서 자동기록된 성취는 직접 수정할 수 없습니다.");
        }
    }

    public void checkDeletable() {
        if (isPlatformVerified()) {
            throw new ServiceException("409-1", "플랫폼에서 자동기록된 성취는 삭제할 수 없습니다.");
        }
    }

    // 사용자가 직접 일으키는 상태 전이. 자동기록 성취는 checkModifiable()에서 이미 막힌다.
    public void changeStatus(GoalStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new ServiceException("409-2", "허용되지 않는 상태 변경입니다.");
        }
        this.status = next;
    }

    // 파티 라이프사이클 이벤트로 시스템이 전이시키는 경로 (자식 엔티티에서만 호출)
    protected void markAchieved() {
        this.status = GoalStatus.ACHIEVED;
    }
}
