package com.back.domain.contest.contest.dtos;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.entity.ContestTag;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContestResponseDto(
        long id,
        Long hostId,
        Long creatorMemberId,
        String title,
        ContestFormat format,
        ContestTag contestTag,
        LocalDate applicationPeriodStart,
        LocalDate applicationPeriodEnd,
        boolean archived,
        String description,
        String imageUrl,
        String linkUrl,
        int likeCount,
        int viewCount,
        LocalDateTime createDate
) {
    public ContestResponseDto(Contest contest, ContestPost contestPost){
        this(
                contest.getId(),
                contest.getHostId(),
                contest.getCreatorMemberId(),
                contest.getTitle(),
                contest.getFormat(),
                contest.getContestTag(),
                contest.getApplicationPeriodStart(),
                contest.getApplicationPeriodEnd(),
                false, // 방금 생성했으니 archived는 항상 false
                contestPost.getDescription(),
                contestPost.getImageUrl(),
                contestPost.getLinkUrl(),
                contestPost.getLikeCount(),
                contestPost.getViewCount(),
                contest.getCreateDate()
        );
    }
}
