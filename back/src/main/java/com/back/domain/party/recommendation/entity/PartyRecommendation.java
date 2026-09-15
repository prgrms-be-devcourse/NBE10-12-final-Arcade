package com.back.domain.party.recommendation.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(indexes = @Index(name = "idx_party_recommendation_member", columnList = "member_id, rank_no"))
public class PartyRecommendation extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private long memberId;

    // Party FK를 걸지 않는다 - 배치가 회원별로 통째로 delete+insert하는 구조라
    // 파티가 삭제돼도 이 테이블이 즉시 정합성을 깨질 필요가 없고, 읽을 때 존재 여부만 걸러낸다.
    @Column(name = "party_id", nullable = false)
    private long partyId;

    @Column(name = "rank_no", nullable = false)
    private int rank;

    @Column(length = 120)
    private String reason;

    public PartyRecommendation(long memberId, long partyId, int rank, String reason) {
        this.memberId = memberId;
        this.partyId = partyId;
        this.rank = rank;
        this.reason = reason;
    }
}
