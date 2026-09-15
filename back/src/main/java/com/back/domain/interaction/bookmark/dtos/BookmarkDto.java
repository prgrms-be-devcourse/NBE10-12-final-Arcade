package com.back.domain.interaction.bookmark.dtos;

import com.back.domain.interaction.like.entity.TargetType;

public record BookmarkDto(
        TargetType targetType,
        long targetId,
        boolean bookmarked
) { }
