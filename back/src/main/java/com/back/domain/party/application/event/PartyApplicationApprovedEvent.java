package com.back.domain.party.application.event;

public record PartyApplicationApprovedEvent(
        long partyId,
        long memberId) { }
