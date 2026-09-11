package com.back.domain.interaction.like.repository;

import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface LikeActionRepository extends JpaRepository<LikeAction, Long> {

    interface TargetLatest {
        Long getTargetId();
        LocalDateTime getLatest();
    }

    @Query("""
        select la.targetId as targetId, count(la) as count
        from LikeAction la
        where la.targetType = :targetType and la.targetId in :targetIds and la.createDate >= :from
        group by la.targetId
        """)
    List<TargetCount> countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(
            @Param("targetType") TargetType targetType,
            @Param("targetIds") Collection<Long> targetIds,
            @Param("from") LocalDateTime from
    );

    @Query("""
        select la.targetId as targetId, max(la.createDate) as latest
        from LikeAction la
        where la.targetType = :targetType and la.targetId in :targetIds
        group by la.targetId
        """)
    List<TargetLatest> findLatestCreateDateGroupedByTargetTypeAndTargetIdIn(
            @Param("targetType") TargetType targetType,
            @Param("targetIds") Collection<Long> targetIds
    );

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
}
