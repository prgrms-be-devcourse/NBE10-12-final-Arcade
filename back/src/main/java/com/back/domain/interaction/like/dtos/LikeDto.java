package com.back.domain.interaction.like.dtos;

import com.back.domain.interaction.like.entity.TargetType;

public record LikeDto(
        TargetType targetType,
        long targetId,
        boolean liked,
        int likeCount
) { }
