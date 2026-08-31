package com.back.domain.party.assemble.entity;

import com.back.domain.party.party.entity.Party;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class PartyAssemble extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false, unique = true)
    private Party party;

    // 이 확정 이벤트가 이벤트 버스에 실제로 발행된 시각(아웃박스 패턴 확인용).
    // 지금은 ApplicationEventPublisher로 동기 발행만 하고 있어 별도 아웃박스 처리는
    // 안 하므로 당장은 값을 채우지 않고 필드만 남겨둔다.
    private LocalDateTime publishedAt;

    public PartyAssemble(Party party) {
        this.party = party;
    }
}
