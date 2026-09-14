package com.back.domain.party.recommendation.dtos;

import java.util.List;

public record PartyRecommendationResultDto(
        boolean profileRequired,
        List<PartyRecommendationItemDto> items
) {
    public static PartyRecommendationResultDto needsProfile() {
        return new PartyRecommendationResultDto(true, List.of());
    }

    public static PartyRecommendationResultDto of(List<PartyRecommendationItemDto> items) {
        return new PartyRecommendationResultDto(false, items);
    }
}
