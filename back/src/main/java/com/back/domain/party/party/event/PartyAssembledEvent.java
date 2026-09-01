package com.back.domain.party.party.event;

import com.back.domain.member.member.entity.PositionType;

import java.time.LocalDate;
import java.util.List;

// 파티가 마감됐을 때 발행되는 이벤트
// 성취 도메인이 이 이벤트를 구독해서 참여자별 PROJECT 타입 Goal을 IN_PROGRESS로 자동 생성하고, 체크리스트 도메인이 이 파티의 체크리스트를 오픈하는 식으로 쓰면 된다
public record PartyAssembledEvent(
        long partyId,
        LocalDate assembledAt,
        List<ApprovedMember> approvedMembers
) {
    public record ApprovedMember(
            long memberId,
            long partyAssembleToMemberId,
            PositionType positionType
    ) {
    }

    // 하위 호환용. 예전엔 이 이벤트가 memberId 리스트만 담고 있었는데 Goal 도메인 코드가 이 필드명을 그대로 쓰고 있어서 새 구조로 넘어가는 동안 컴파일이 깨지지 않도록 예전 접근 방식을 그대로 남겨둔다.
    // Goal 쪽이 마이그레이션하면 이 메서드는 지워야 함
    @Deprecated
    public List<Long> approvedMemberIds() {
        return approvedMembers.stream()
                .map(ApprovedMember::memberId)
                .toList();
    }
}
