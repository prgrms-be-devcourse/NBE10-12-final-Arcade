package com.back.domain.party.recommendation.dtos;

import com.back.domain.party.party.dtos.PartyListItemDto;

public record PartyRecommendationItemDto(
        PartyListItemDto party,
        String reason
) {}
