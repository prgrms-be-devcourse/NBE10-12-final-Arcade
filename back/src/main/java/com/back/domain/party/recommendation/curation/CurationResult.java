package com.back.domain.party.recommendation.curation;

public record CurationResult(long partyId, int rank, String reason) {
}
