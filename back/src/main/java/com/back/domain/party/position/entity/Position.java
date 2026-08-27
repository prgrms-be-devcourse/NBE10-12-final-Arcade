package com.back.domain.party.position.entity;

import com.back.domain.party.party.entity.Party;
import com.back.global.exception.ServiceException;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Position extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PositionType type;

    @Column(nullable = false)
    private int filledCount;

    @Column(nullable = false)
    private int capacity;

    // 정원 초과 방지용 낙관적 락 (본인 중복 클릭/다중 탭 시나리오 방지 목적)
    @Version
    private int version;

    public Position(PositionType type, int capacity) {
        this.type = type;
        this.capacity = capacity;
        this.filledCount = 0;
    }

    public void assignParty(Party party) {
        this.party = party;
    }

    public boolean hasVacancy() {
        return filledCount < capacity;
    }

    // 승인 시 호출. 정원 초과면 예외로 막는다 (@Version 충돌은 서비스 레이어에서 별도로 409-2 처리).
    public void fillOneSeat() {
        if (!hasVacancy()) {
            throw new ServiceException("409-2", "정원이 마감되어 승인할 수 없습니다. 새로고침 후 다시 시도해주세요.");
        }
        this.filledCount++;
    }

    // 정원은 늘리는 수정만 허용 - 이미 승인된 인원보다 작게 줄일 수 없음
    public void changeCapacity(int newCapacity) {
        if (newCapacity < this.filledCount) {
            throw new ServiceException("400-4", "정원은 현재 승인된 인원보다 작게 설정할 수 없습니다.");
        }
        this.capacity = newCapacity;
    }
}
