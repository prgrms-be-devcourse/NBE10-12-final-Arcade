package com.back.domain.party.application.dtos;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.party.entity.Party;

/**
 * 마이페이지에서 조회하는 내 지원 상태
 * party.id 는 카드에서 파티로 이동하는 데 쓴다.
 */
public record MyApplicationDto(
        PartySummary party,
        PositionType position,
        PartyMemberStatus state
) {
    public record PartySummary(long id, String name) {
        public PartySummary(Party party) {
            this(party.getId(), party.getPartyName());
        }
    }

    public MyApplicationDto(PartyMember partyMember) {
        this(
                new PartySummary(partyMember.getParty()),
                partyMember.getPosition().getType(),
                partyMember.getStatus()
        );
    }
}
