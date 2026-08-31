package com.back.domain.interaction.like.repository;

import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeActionRepository extends JpaRepository<LikeAction, Long> {
    Optional<LikeAction> findByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    @Query("select la.targetId from LikeAction la where la.member = :member and la.targetType = :targetType")
    List<Long> findTargetIdsByMemberAndTargetType(@Param("member") Member member, @Param("targetType") TargetType targetType);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LikeAction la where la.targetType = :targetType and la.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(@Param("targetType") TargetType targetType, @Param("targetId") long targetId);
}
