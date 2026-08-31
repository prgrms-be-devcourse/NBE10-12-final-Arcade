package com.back.domain.contest.contest.repository;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.entity.ContestTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ContestPostRepository extends JpaRepository<ContestPost, Long> {
    Optional<ContestPost> findByContest(Contest contest);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ContestPost cp where cp.contest.applicationPeriodEnd < :date")
    void deleteAllByContest_ApplicationPeriodEndBefore(@Param("date") LocalDate date);

    // 정렬기준
    @Query(value = """
        select cp from ContestPost cp
        join fetch cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        order by c.createDate desc, cp.id desc
        """,
        countQuery = """
        select count(cp) from ContestPost cp
        join cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        """)
    Page<ContestPost> searchOrderByLatest(
            @Param("format") ContestFormat format,
            @Param("contestTag") ContestTag contestTag,
            Pageable pageable
    );

    @Query(value = """
        select cp from ContestPost cp
        join fetch cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        order by cp.likeCount desc, cp.id desc
        """,
        countQuery = """
        select count(cp) from ContestPost cp
        join cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        """)
    Page<ContestPost> searchOrderByPopular(
            @Param("format") ContestFormat format,
            @Param("contestTag") ContestTag contestTag,
            Pageable pageable
    );

    @Query(value = """
        select cp from ContestPost cp
        join fetch cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        order by c.applicationPeriodEnd asc, cp.id desc
        """,
        countQuery = """
        select count(cp) from ContestPost cp
        join cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        """)
    Page<ContestPost> searchOrderByDeadline(
            @Param("format") ContestFormat format,
            @Param("contestTag") ContestTag contestTag,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ContestPost cp set cp.viewCount = cp.viewCount + 1 where cp.contest.id = :contestId")
    void increaseViewCount(@Param("contestId") long contestId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ContestPost cp set cp.likeCount = cp.likeCount + 1 where cp.contest.id = :contestId")
    void increaseLikeCount(@Param("contestId") long contestId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ContestPost cp set cp.likeCount = case when cp.likeCount > 0 then cp.likeCount - 1 else 0 end where cp.contest.id = :contestId")
    void decreaseLikeCount(@Param("contestId") long contestId);
}
