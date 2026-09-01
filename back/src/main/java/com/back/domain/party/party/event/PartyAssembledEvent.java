package com.back.domain.party.party.event;

import java.util.List;

// 파티가 마감됐을 때 발행되는 이벤트
// 성취 도메인이 이 이벤트를 구독해서 참여자별 PROJECT 타입 Goal을 IN_PROGRESS로 자동 생성하고, 체크리스트 도메인이 이 파티의 체크리스트를 오픈하는 식으로 쓰면 된다
public record PartyAssembledEvent(
        long partyId,
        List<Long> approvedMemberIds
) {
}
