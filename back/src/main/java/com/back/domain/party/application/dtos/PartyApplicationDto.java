package com.back.domain.party.application.dtos;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;

import java.time.LocalDateTime;

public record PartyApplicationDto(
        long id,
        long partyId,
        long applicantId,
        String applicantName,
        long positionId,
        PositionType positionType,
        PartyMemberStatus status,
        String message,
        LocalDateTime createDate
) {
    public PartyApplicationDto(PartyMember partyMember) {
        this(
                partyMember.getId(),
                partyMember.getParty().getId(),
                partyMember.getMember().getId(),
                partyMember.getMember().getName(),
                partyMember.getPosition().getId(),
                partyMember.getPosition().getType(),
                partyMember.getStatus(),
                partyMember.getMessage(),
                partyMember.getCreateDate()
        );
    }
}
