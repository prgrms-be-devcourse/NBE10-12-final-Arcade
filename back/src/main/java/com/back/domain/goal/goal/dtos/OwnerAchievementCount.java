package com.back.domain.goal.goal.dtos;

/**
 * 회원별 성취 건수를 출처로 나눈 집계. 목록 화면이 건수만 쓸 때 Goal 을 통째로 읽지 않으려고 둔다.
 *
 * Goal 은 JOINED 상속이라 엔티티로 읽으면 자식 테이블 3개를 조인한다.
 */
public record OwnerAchievementCount(
        long ownerId,
        /** 크루온 활동으로 자동기록된 건 - 지금은 파티 확정으로 생기는 PROJECT 뿐이다 */
        long platformVerified,
        /** 사용자가 직접 등록한 건 - 수상·체크리스트 */
        long selfReported
) {
}
