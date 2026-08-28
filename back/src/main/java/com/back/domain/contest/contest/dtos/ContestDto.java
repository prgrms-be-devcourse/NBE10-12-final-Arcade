package com.back.domain.contest.contest.dtos;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContestDto(
    long id,
    Long creatorMemberId,
    String name,
    String description,
    ContestFormat format,
    ContestTag contestTag,
    LocalDate applicationPeriodStart,
    LocalDate applicationPeriodEnd,
    String linkUrl,
    int likeCount,
    int viewCount,
    String imageUrl,
    LocalDateTime createDate,
    LocalDateTime modifyDate
) {
    public ContestDto(Contest contest) {
        this(
            contest.getId(),
            contest.getCreatorMemberId(),
            contest.getName(),
            contest.getDescription(),
            contest.getFormat(),
            contest.getContestTag(),
            contest.getApplicationPeriodStart(),
            contest.getApplicationPeriodEnd(),
            contest.getLinkUrl(),
            contest.getLikeCount(),
            contest.getViewCount(),
            contest.getImageUrl(),
            contest.getCreateDate(),
            contest.getModifyDate()
        );
    }
}
