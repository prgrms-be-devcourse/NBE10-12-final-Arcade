package com.back.domain.party.party.event;

import java.time.LocalDateTime;

// 파티가 완료 처리됐을 때 발행되는 이벤트
// 성취 도메인이 이 이벤트를 구독해 기존 PROJECT 타입 Goal을 ACHIEVED로 전이시키고 endDate를 채운다.
// 참여자 목록은 Goal.sourcePartyId로 되짚을 수 있으므로 이벤트엔 partyId만 담는다.
//
// completedAt을 함께 싣는 이유는, 리스너가 커밋 이후에 돌고 재실행될 수도 있어서다.
// 리스너가 LocalDateTime.now()를 쓰면 재처리할 때마다 종료일이 달라진다.
public record PartyCompletedEvent(
        long partyId,
        LocalDateTime completedAt
) {
}
