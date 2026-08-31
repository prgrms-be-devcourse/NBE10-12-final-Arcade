package com.back.domain.interaction.bookmark.repository;

import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    boolean existsByMemberAndTargetTypeAndTargetId(Member member, TargetType targetType, long targetId);

    @Query("select b.targetId from Bookmark b where b.member = :member and b.targetType = :targetType")
    List<Long> findTargetIdsByMemberAndTargetType(@Param("member") Member member, @Param("targetType") TargetType targetType);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Bookmark b where b.targetType = :targetType and b.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(@Param("targetType") TargetType targetType, @Param("targetId") long targetId);
}
