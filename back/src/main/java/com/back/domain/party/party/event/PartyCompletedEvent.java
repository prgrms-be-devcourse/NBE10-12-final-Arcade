package com.back.domain.party.party.event;

// 파티가 완료 처리됐을 때 발행되는 이벤트
// 성취 도메인이 이 이벤트를 구독해 기존 PROJECT 타입 Goal을 ACHIEVED로 전이시키고 endDate를 채우면 된다
// 지금은 리스너가 없어 발행만 된다. 참여자 목록은 PartyAssembleToMember를 통해 조회 가능하므로 이벤트엔 partyId만 담는다.
public record PartyCompletedEvent(
        long partyId
) {
}
