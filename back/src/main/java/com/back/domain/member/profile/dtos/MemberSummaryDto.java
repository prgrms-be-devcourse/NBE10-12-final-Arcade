package com.back.domain.member.profile.dtos;

import java.util.List;

/**
 * 마이페이지 '활동 스코어' 카드가 쓰는 집계값.
 *
 * 프로필(MemberProfileDto)과 나눠 둔 이유는 GET /members/me 가 세션 확인용이라
 * 거의 모든 화면이 부르고, PATCH /members/me 도 같은 DTO 를 돌려주기 때문이다.
 * 여기 섞으면 닉네임 한 줄 고칠 때마다 집계 쿼리가 함께 돈다.
 */
public record MemberSummaryDto(
        /** 승인된 파티원으로 속한 파티 중 COMPLETED 인 건 */
        long completedParties,
        /** 달성한 CONTEST 성취 건수 */
        long awards,
        /** 승인된 파티원으로 속한 파티 중 전시가 게시된 건 */
        long exhibitions,
        /** 연속 활동일. 일자별 활동 기록 도메인이 없어 아직 0 고정이다. */
        int streakDays,
        /** 획득 배지. 배지 도메인이 없어 아직 빈 배열이다. */
        List<String> badges
) {
    public MemberSummaryDto(long completedParties, long awards, long exhibitions) {
        this(completedParties, awards, exhibitions, 0, List.of());
    }
}
