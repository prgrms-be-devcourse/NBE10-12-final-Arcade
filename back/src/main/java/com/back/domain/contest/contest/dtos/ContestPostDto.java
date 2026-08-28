package com.back.domain.contest.contest.dtos;

import com.back.domain.contest.contest.entity.ContestPost;

public record ContestPostDto(
        long id,
        String description,
        String linkUrl,
        int likeCount,
        int viewCount,
        String imageUrl
) {
    public ContestPostDto(ContestPost contestPost){
        this(
                contestPost.getId(),
                contestPost.getDescription(),
                contestPost.getLinkUrl(),
                contestPost.getLikeCount(),
                contestPost.getViewCount(),
                contestPost.getImageUrl()
        );

    }
}
