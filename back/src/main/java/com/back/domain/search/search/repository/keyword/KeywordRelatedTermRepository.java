package com.back.domain.search.search.repository.keyword;

import com.back.domain.search.search.entity.keyword.KeywordRelatedTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KeywordRelatedTermRepository extends JpaRepository<KeywordRelatedTerm, Long> {
    List<KeywordRelatedTerm> findByTerm(String term);
}
