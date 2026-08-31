package com.back.domain.interaction.bookmark.service;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;

import java.util.Collection;
import java.util.Set;

public interface BookmarkInteractionPort {
    void deleteAllBookmarksForTarget(TargetType targetType, long targetId);

    Set<Long> findBookmarkedTargetIds(Member member, TargetType targetType, Collection<Long> targetIds);
}
