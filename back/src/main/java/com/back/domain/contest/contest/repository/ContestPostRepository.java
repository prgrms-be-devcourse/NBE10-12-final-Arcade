package com.back.domain.contest.contest.repository;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.entity.ContestTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContestPostRepository extends JpaRepository<ContestPost, Long> {
    Optional<ContestPost> findByContest(Contest contest);
    List<ContestPost> findAllByContest_ApplicationPeriodEndBefore(LocalDate date);

    // ContestPost가 있는(=archived 아닌) 대회만 조회 대상. 정렬이 쿼리에 고정돼 있으므로
    // 호출 시 Pageable은 반드시 Sort.unsorted()로 넘겨야 한다 (Party.searchOrderByVacancy와 동일한 이유).
    @Query(value = """
        select cp from ContestPost cp
        join cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        order by c.createDate desc
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
        join cp.contest c
        where (:format is null or c.format = :format)
          and (:contestTag is null or c.contestTag = :contestTag)
        order by cp.likeCount desc
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
}
