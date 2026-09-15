package com.back.domain.contest.contest.repository;

import com.back.domain.contest.contest.entity.Contest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContestLookupPort {

    @Query("select c from Contest c where c.id = :contestId")
    Optional<Contest> findContestById(@Param("contestId") long contestId);
}
