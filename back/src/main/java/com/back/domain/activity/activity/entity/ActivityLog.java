package com.back.domain.activity.activity.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 회원의 하루치 활동 기록(기획서 3.11). 스트릭 판정과 마이페이지 히트맵이 이 테이블 하나를 같이 쓴다.
 *
 * 무엇을 활동으로 세는지는 기획서 2.9가 정한다 - 파티 생성·지원·승인/거절 처리·모집 마감·완료 판정,
 * 그리고 파티 대상 좋아요·북마크. 넓게 잡아 두고 악용 사례가 나오면 그때 조인다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "activity_date"}))
public class ActivityLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 활동이 있었던 날. 하루 경계는 KST 기준이다 */
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    /** 그날 발생한 활동 횟수. 스트릭은 0보다 크기만 하면 되고, 히트맵 농도가 이 값을 쓴다 */
    @Column(nullable = false)
    private int count;

    public ActivityLog(Member member, LocalDate activityDate, int count) {
        this.member = member;
        this.activityDate = activityDate;
        this.count = count;
    }

    // 동시 요청이 겹치면 증가분이 하나 묻힐 수 있다. count 는 히트맵 농도를 0~3 구간으로 나누는 데만
    // 쓰이고 스트릭은 존재 여부만 보므로, 원자적 UPDATE 를 쓸 만큼의 정확도가 필요하지 않다.
    // ponytail: 더티체킹 증가, 값이 화면 이상으로 쓰이면 원자적 UPDATE 로 바꿀 것
    public void increase() {
        this.count++;
    }
}
