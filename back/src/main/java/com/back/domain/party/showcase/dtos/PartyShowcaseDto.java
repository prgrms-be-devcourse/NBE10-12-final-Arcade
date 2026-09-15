package com.back.domain.party.showcase.dtos;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record PartyShowcaseDto(
        long partyId,
        String partyName,
        String ownerName,
        List<String> memberNames,
        String githubRepoUrl,
        String title,
        String description,
        boolean published,
        LocalDateTime publishedAt,
        int viewCount,
        int likeCount,
        List<PrSummary> pullRequests
) {
    public record PrSummary(
            int number,
            String title,
            String htmlUrl,
            String state,
            String authorLogin,
            boolean merged,
            OffsetDateTime mergedAt
    ) {
    }
}
