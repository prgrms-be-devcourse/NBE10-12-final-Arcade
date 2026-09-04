package com.back.domain.goal.goal.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.global.exception.ServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 파티 확정(모집 마감) 시 시스템이 참여자별로 자동 생성하는 전용 타입.
// 사용자가 직접 등록할 수 없다 - 개인 사이드 프로젝트는 PersonalChecklist로 남긴다(3.6).
//
// '파티장이었는지'는 저장하지 않는다 - sourcePartyId로 파티를 조회해 owner와 비교하면 알 수 있고, 바뀌지 않는 값이다.
@Entity
@Getter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "goal_id")
public class Project extends Goal {

    // 파티 이름(PARTY.partyName). 확정 시점의 값을 복사한다 - 목록에서 성취마다 파티를 다시 읽지 않으려고.
    @Column(nullable = false)
    private String title;

    // 전시 게시글. 게시 전이면 null이고, 게시되는 순간 리스너가 채운다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_showcase_id")
    private PartyShowcase partyShowcase;

    // 이 파티에서 맡은 포지션. 파티장은 지원 절차를 거치지 않아 포지션이 정해져 있지 않으므로 null이다.
    // 파티 생성 시 파티장도 자기 포지션을 고르게 되면 그때 채운다.
    @Enumerated(EnumType.STRING)
    private PositionType positionType;

    // 파티 확정 시점
    @Column(nullable = false)
    private LocalDate startDate;

    // 파티 완료 시점. 진행중이면 null
    private LocalDate endDate;

    public Project(
            Member owner,
            Long partyAssembleToMemberId,
            Long sourcePartyId,
            String title,
            PositionType positionType,
            LocalDate startDate
    ) {
        // 아직 결과물이 없는 시작 시점이라 ACHIEVED가 아니라 IN_PROGRESS로 생성한다(3.6).
        super(owner, GoalType.PROJECT, GoalStatus.IN_PROGRESS, GoalSource.PLATFORM_VERIFIED, partyAssembleToMemberId, sourcePartyId);
        this.title = title;
        this.positionType = positionType;
        this.startDate = startDate;
    }

    // 파티 완료 이벤트 수신 시 호출. 상태 전이와 종료일 기록을 함께 처리한다.
    public void complete(LocalDate endDate) {
        if (getStatus() == GoalStatus.ACHIEVED) {
            throw new ServiceException("409-2", "이미 완료 처리된 성취입니다.");
        }
        markAchieved();
        this.endDate = endDate;
    }

    /**
     * 전시 게시글을 연결한다. 전시글은 파티당 하나인데 Project는 참여자 수만큼 있어
     * 그 파티의 Project 전체에 대해 호출한다.
     *
     * 이미 같은 전시글이 걸려 있으면 아무 것도 하지 않는다 - 이벤트를 두 번 받아도 안전해야 한다.
     */
    public void linkShowcase(PartyShowcase partyShowcase) {
        this.partyShowcase = partyShowcase;
    }

    // PROJECT는 완료(ACHIEVED)만으로는 부족하고 파티장이 실제로 전시글을 게시해서 partyShowcase가 연결돼 있어야 전시된 것으로 본다.
    @Override
    public boolean isExhibited() {
        return super.isExhibited() && this.partyShowcase != null;
    }
}
