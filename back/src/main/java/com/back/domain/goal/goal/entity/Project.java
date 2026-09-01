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
// '파티장이었는지'는 저장하지 않는다 - sourcePartyId로 파티를 조회해 owner와 비교하면 알 수 있고, 바뀌지 않는 값이다.
@Entity
@Getter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "goal_id")
public class Project extends Goal {

    // [추후 PARTY_SHOWCASE.title로 채워짐 - 작업표 34번]
    //
    // 이 값의 출처는 전시 게시글(PARTY_SHOWCASE.title)이고, 그 테이블은 아직 없다.
    // PARTY_SHOWCASE.title 자체가 파티장이 게시를 확정해야 채워지는 값이라 파티 확정 시점에는 가져올 것이 없으므로,
    // 생성 시에는 null로 두고 전시 게시가 이뤄질 때 updateShowcase()로 채운다.
    //
    // 조회할 때마다 파티를 다시 읽지 않으려고 값을 들고 있는 구조다.
    // 대신 참여자 수만큼 같은 값이 복사되므로, 전시 게시글이 수정되면 이 파티의 Project 전체를 함께 갱신해야 한다.
    // 갱신 경로는 updateShowcase() 하나로 통일한다 - 다른 데서 title/result를 직접 건드리지 말 것.
    //
    // TODO 전시 게시를 하지 않은 파티는 title이 계속 null이다.
    //      성취 카드에 제목이 비어 보이므로, 34번을 붙일 때 화면에서 무엇을 대신 보여줄지 정해야 한다.
    private String title;

    // [추후 PARTY_SHOWCASE.description으로 채워짐 - 작업표 34번]
    // title과 동일하게 전시 게시 시점에 채워진다.
    private String result;

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
     * 전시 게시글(PARTY_SHOWCASE)의 제목·설명을 이 성취에 반영한다.
     * PARTY_SHOWCASE.title -> title, PARTY_SHOWCASE.description -> result 로 매핑한다(기획서 3.6).
     *
     * 전시 게시 확정 API(POST /api/v1/parties/{partyId}/showcase, 작업표 34번)를 구현할 때
     * 게시글 저장 직후 같은 트랜잭션에서 호출해야 한다.
     *
     * 전시 게시글은 파티당 1개인데 Project는 참여자 수만큼 있으므로,
     * 해당 파티의 Project 전체에 대해 호출해야 한다.
     * 하나라도 빠지면 파티원마다 다른 제목이 보이고 에러는 나지 않는다.
     */
    public void updateShowcase(String title, String result) {
        this.title = title;
        this.result = result;
    }
}
