package com.back.domain.contest.contest.repository;

import com.back.domain.contest.contest.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRepository extends JpaRepository<Contest, Long> {
}
