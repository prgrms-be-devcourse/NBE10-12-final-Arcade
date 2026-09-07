package com.back.domain.party.application.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.position.entity.Position;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "member_id"}))
public class PartyMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyMemberStatus status;

    @Column(length = 50)
    private String message;

    public PartyMember(Party party, Member member, Position position, String message) {
        this.party = party;
        this.member = member;
        this.position = position;
        this.message = message;
        this.status = PartyMemberStatus.PENDING;
    }

    // 파티장은 지원 절차를 거치지 않는다. 처음부터 확정 멤버라야
    // 모집 마감 시 확정 명단(PartyAssembleToMember)과 성취 자동생성에 포함된다.
    public static PartyMember owner(Party party, Member owner, Position position) {
        PartyMember partyMember = new PartyMember(party, owner, position, null);
        partyMember.status = PartyMemberStatus.APPROVED;

        return partyMember;
    }

    public void approve() {
        if (this.status != PartyMemberStatus.PENDING) {
            throw new ServiceException("409-1", "이미 처리된 지원입니다.");
        }
        this.status = PartyMemberStatus.APPROVED;
    }

    public void reject() {
        if (this.status != PartyMemberStatus.PENDING) {
            throw new ServiceException("409-1", "이미 처리된 지원입니다.");
        }
        this.status = PartyMemberStatus.REJECTED;
    }

    // 이미 승인된 파티원을 다시 REJECTED로 되돌린다. 파티 삭제는 승인된 파티원이 하나도 없어야
    // 가능한데(실제 팀 소속 관계라 그냥 지워버릴 수 없음), 그러려면 먼저 이 메서드로 승인을 하나씩
    // 취소해서 승인 인원을 0으로 만들어야 한다.
    public void cancelApproval() {
        if (this.status != PartyMemberStatus.APPROVED) {
            throw new ServiceException("409-1", "승인된 지원 건만 취소할 수 있습니다.");
        }
        this.status = PartyMemberStatus.REJECTED;
    }
}
