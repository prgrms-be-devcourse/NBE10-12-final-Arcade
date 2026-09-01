package com.back.domain.contest.contest.dtos;

import com.back.domain.contest.contest.entity.Contest;

public record ContestSummaryDto(
        long id,
        String title
) {
    public ContestSummaryDto(Contest contest) {
        this(contest.getId(), contest.getTitle());
    }
}
