package com.back.domain.contest.contest.repository;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContestPostRepository extends JpaRepository<ContestPost, Long> {
    Optional<ContestPost> findByContest(Contest contest);
    List<ContestPost> findAllByContest_ApplicationPeriodEndBefore(LocalDate date);
}
