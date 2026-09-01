package com.back.domain.goal.goal.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.global.exception.ServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 파티 확정(모집 마감) 시 시스템이 참여자별로 자동 생성하는 전용 타입.
// 사용자가 직접 등록할 수 없다 - 개인 사이드 프로젝트는 PersonalChecklist로 남긴다(3.6).
//
// 사람마다 다른 값만 갖는다. 제목·설명·PR 이력·좋아요처럼 파티원 전원이 공유하는 값은
// sourcePartyId가 가리키는 PARTY/PARTY_SHOWCASE/PARTY_PR에 한 벌만 두고 조회 시 조립한다.
// 여기에 복사해두면 파티장이 전시 게시글을 고칠 때마다 참여자 수만큼 갱신해야 하고, 하나라도 빠지면 사람마다 다른 내용이 보인다.
//
// '파티장이었는지'도 저장하지 않는다 - sourcePartyId로 파티를 조회해 owner와 비교하면 알 수 있고, 바뀌지 않는 값이다.
@Entity
@Getter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "goal_id")
public class Project extends Goal {

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
            PositionType positionType,
            LocalDate startDate
    ) {
        // 아직 결과물이 없는 시작 시점이라 ACHIEVED가 아니라 IN_PROGRESS로 생성한다(3.6).
        super(owner, GoalType.PROJECT, GoalStatus.IN_PROGRESS, GoalSource.PLATFORM_VERIFIED, partyAssembleToMemberId, sourcePartyId);
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
}
