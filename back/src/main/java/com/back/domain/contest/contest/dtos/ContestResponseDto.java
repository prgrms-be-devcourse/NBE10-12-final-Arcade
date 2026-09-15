package com.back.domain.contest.contest.dtos;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.party.party.dtos.PartyListItemDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        Integer likeCount,
        Integer viewCount,
        boolean bookmarkedByMe,
        boolean likedByMe,
        LocalDateTime createDate,
        int teams,
        List<PartyListItemDto> relatedParties
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
                contestPost == null,
                contestPost == null ? null : contestPost.getDescription(),
                contestPost == null ? null : contestPost.getImageUrl(),
                contestPost == null ? null : contestPost.getLinkUrl(),
                contestPost == null ? null : contestPost.getLikeCount(),
                contestPost == null ? null : contestPost.getViewCount(),
                false, // bookmarkedByMe
                false, // likedByMe
                contest.getCreateDate(),
                0, // teams
                List.of() // relatedParties
        );
    }

    public ContestResponseDto withMyInteractions(boolean bookmarkedByMe, boolean likedByMe) {
        return new ContestResponseDto(
                id, hostId, creatorMemberId, title, format, contestTag,
                applicationPeriodStart, applicationPeriodEnd, archived,
                description, imageUrl, linkUrl, likeCount, viewCount,
                bookmarkedByMe, likedByMe, createDate,teams, relatedParties
        );
    }

    public ContestResponseDto withRelatedParties(int teams, List<PartyListItemDto> relatedParties) {
        return new ContestResponseDto(id, hostId, creatorMemberId, title, format, contestTag,
                applicationPeriodStart, applicationPeriodEnd, archived,
                description, imageUrl, linkUrl, likeCount, viewCount,
                bookmarkedByMe, likedByMe, createDate, teams, relatedParties
        );
    }
}
