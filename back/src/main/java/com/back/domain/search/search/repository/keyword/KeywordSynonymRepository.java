package com.back.domain.search.search.repository.keyword;

import com.back.domain.search.search.entity.keyword.KeywordSynonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KeywordSynonymRepository extends JpaRepository<KeywordSynonym, Long> {
    Optional<KeywordSynonym> findByTerm(String term);

    List<KeywordSynonym> findByTermIn(Collection<String> terms);
}
