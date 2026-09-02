package com.back.domain.party.showcase.event;

// 파티 전시 게시가 완료됐을 때 발행되는 이벤트
// 성취 도메인이 이 이벤트를 구독해 해당 파티의 Project 전체에 Project.updateShowcase를 반영하면 된다, 지금은 리스너가 없어 발행만 된다.
public record PartyShowcasePublishedEvent(
        long partyId,
        String title,
        String description
) {
}
