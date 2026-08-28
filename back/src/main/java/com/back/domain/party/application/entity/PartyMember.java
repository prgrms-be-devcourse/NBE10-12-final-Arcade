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

// 지원(신청)과 매칭 확정(가입)을 한 테이블에 state로 통합 (기획서 3.5)
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
}
