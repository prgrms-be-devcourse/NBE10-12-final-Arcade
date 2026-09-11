package com.back.domain.party.party.dtos;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.position.entity.PartyStatus;

import java.time.LocalDateTime;
import java.util.List;

public record MemberPartyHistoryDto(
        List<OwnedPartyItem> ownedParties,
        List<AppliedPartyItem> appliedParties
) {
    public record OwnedPartyItem(
            long partyId,
            String partyName,
            String title,
            PartyStatus status,
            boolean hidden,
            LocalDateTime createDate
    ) {
        public OwnedPartyItem(Party party) {
            this(party.getId(), party.getPartyName(), party.getTitle(),
                    party.getStatus(), party.isHidden(), party.getCreateDate());
        }
    }

    public record AppliedPartyItem(
            long partyId,
            String partyName,
            String title,
            PositionType position,
            PartyMemberStatus applicationStatus,
            String message,
            LocalDateTime appliedAt
    ) {
        public AppliedPartyItem(PartyMember partyMember) {
            this(
                    partyMember.getParty().getId(),
                    partyMember.getParty().getPartyName(),
                    partyMember.getParty().getTitle(),
                    partyMember.getPosition().getType(),
                    partyMember.getStatus(),
                    partyMember.getMessage(),
                    partyMember.getCreateDate()
            );
        }
    }
}
