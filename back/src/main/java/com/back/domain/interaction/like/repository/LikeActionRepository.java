package com.back.domain.interaction.like.repository;

import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeActionRepository extends JpaRepository<LikeAction, Long> {
    Optional<LikeAction> findByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);
}
