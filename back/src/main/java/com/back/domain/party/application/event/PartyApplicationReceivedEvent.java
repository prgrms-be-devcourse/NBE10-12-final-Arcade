package com.back.domain.party.application.event;

public record PartyApplicationReceivedEvent(
        long partyId,
        long memberId
) { }
