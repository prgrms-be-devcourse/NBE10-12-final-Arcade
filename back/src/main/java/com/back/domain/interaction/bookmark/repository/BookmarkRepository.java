package com.back.domain.interaction.bookmark.repository;

import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    interface TargetCount {
        Long getTargetId();
        Long getCount();
    }

    // TOP3 배치 - 대상별로 따로 쿼리 날리지 않고 창 안의 북마크 수를 한 번에 집계한다.
    @Query("""
        select b.targetId as targetId, count(b) as count
        from Bookmark b
        where b.targetType = :targetType and b.targetId in :targetIds and b.createDate >= :from
        group by b.targetId
        """)
    List<TargetCount> countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(
            @Param("targetType") TargetType targetType,
            @Param("targetIds") Collection<Long> targetIds,
            @Param("from") LocalDateTime from
    );
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
}
