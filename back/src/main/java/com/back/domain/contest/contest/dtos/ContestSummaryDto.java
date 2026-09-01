package com.back.domain.contest.contest.dtos;

import com.back.domain.contest.contest.entity.Contest;

public record ContestSummaryDto( //party에 값 내려주기 위한 dto
        long id,
        String title
) {
    public ContestSummaryDto(Contest contest) {
        this(contest.getId(), contest.getTitle());
    }
}
