package com.back.domain.interaction.bookmark.repository;

import java.time.LocalDateTime;
import com.back.domain.activity.activity.dtos.MemberActivityAt;
import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    // 북마크함 목록. 최근에 담은 것부터 보여준다.
    Page<Bookmark> findAllByMemberOrderByCreateDateDesc(Member member, Pageable pageable);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    @Query("select b.targetId from Bookmark b where b.member = :member and b.targetType = :targetType and b.targetId in :targetIds")
    List<Long> findTargetIdsByMemberAndTargetTypeAndTargetIdIn(
            @Param("member") Member member,
            @Param("targetType") TargetType targetType,
            @Param("targetIds") Collection<Long> targetIds
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Bookmark b where b.member = :member and b.targetType = :targetType and b.targetId = :targetId")
    void deleteByMemberAndTargetTypeAndTargetId(@Param("member") Member member, @Param("targetType") TargetType targetType, @Param("targetId") long targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Bookmark b where b.targetType = :targetType and b.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(@Param("targetType") TargetType targetType, @Param("targetId") long targetId);

    // ACTIVITY_LOG 백필용. 파티 대상 북마크만 활동으로 센다(기획서 2.9).
    @Query("""
            select new com.back.domain.activity.activity.dtos.MemberActivityAt(b.member.id, b.createDate)
            from Bookmark b
            where b.targetType = :targetType and b.createDate >= :from
            """)
    List<MemberActivityAt> findActivityAtSince(
            @Param("targetType") TargetType targetType, @Param("from") LocalDateTime from);
}
