package com.back.domain.interaction.like.service;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;

import java.util.Collection;
import java.util.Set;

public interface LikeInteractionPort {
    void deleteAllLikesForTarget(TargetType targetType, long targetId);

    Set<Long> findLikedTargetIds(Member member, TargetType targetType, Collection<Long> targetIds);
}
