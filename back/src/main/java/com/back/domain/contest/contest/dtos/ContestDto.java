package com.back.domain.contest.contest.dtos;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContestDto(
    long id,
    long hostId,
    long creatorMemberId,
    String title,
    ContestFormat format,
    ContestTag contestTag,
    LocalDate applicationPeriodStart,
    LocalDate applicationPeriodEnd,
    LocalDateTime createDate,
    LocalDateTime modifyDate
) {
    public ContestDto(Contest contest) {
        this(
            contest.getId(),
            contest.getHostId(),
            contest.getCreatorMemberId(),
            contest.getTitle(),
            contest.getFormat(),
            contest.getContestTag(),
            contest.getApplicationPeriodStart(),
            contest.getApplicationPeriodEnd(),
            contest.getCreateDate(),
            contest.getModifyDate()
        );
    }
}
