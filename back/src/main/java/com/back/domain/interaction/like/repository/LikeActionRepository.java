package com.back.domain.interaction.like.repository;

import java.time.LocalDateTime;
import com.back.domain.activity.activity.dtos.MemberActivityAt;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LikeActionRepository extends JpaRepository<LikeAction, Long> {
    Optional<LikeAction> findByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    @Query("select la.targetId from LikeAction la where la.member = :member and la.targetType = :targetType and la.targetId in :targetIds")
    List<Long> findTargetIdsByMemberAndTargetTypeAndTargetIdIn(
            @Param("member") Member member,
            @Param("targetType") TargetType targetType,
            @Param("targetIds") Collection<Long> targetIds
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LikeAction la where la.member = :member and la.targetType = :targetType and la.targetId = :targetId")
    void deleteByMemberAndTargetTypeAndTargetId(@Param("member") Member member, @Param("targetType") TargetType targetType, @Param("targetId") long targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LikeAction la where la.targetType = :targetType and la.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(@Param("targetType") TargetType targetType, @Param("targetId") long targetId);

    // ACTIVITY_LOG 백필용. 파티 대상 좋아요만 활동으로 센다(기획서 2.9).
    @Query("""
            select new com.back.domain.activity.activity.dtos.MemberActivityAt(l.member.id, l.createDate)
            from LikeAction l
            where l.targetType = :targetType and l.createDate >= :from
            """)
    List<MemberActivityAt> findActivityAtSince(
            @Param("targetType") TargetType targetType, @Param("from") LocalDateTime from);
}
