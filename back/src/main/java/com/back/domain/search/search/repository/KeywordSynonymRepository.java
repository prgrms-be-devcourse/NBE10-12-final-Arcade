package com.back.domain.search.search.repository;

import com.back.domain.search.search.entity.KeywordSynonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KeywordSynonymRepository extends JpaRepository<KeywordSynonym, Long> {
    Optional<KeywordSynonym> findByTerm(String term);
}
