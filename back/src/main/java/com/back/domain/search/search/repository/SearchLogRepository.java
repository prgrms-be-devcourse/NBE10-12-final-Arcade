package com.back.domain.search.search.repository;

import com.back.domain.search.search.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
}
