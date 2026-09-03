package com.back.domain.search.search.service.keyword;

import com.back.domain.search.search.entity.keyword.KeywordSynonym;
import com.back.domain.search.search.repository.keyword.KeywordSynonymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordNormalizationService implements KeywordNormalizationPort {

    private final KeywordSynonymRepository keywordSynonymRepository;

    @Override
    public List<String> normalize(List<String> keywords) {
        return keywords.stream()
                .map(this::canonicalize)
                .distinct()
                .toList();
    }

    private String canonicalize(String keyword) {
        return keywordSynonymRepository.findByTerm(keyword)
                .map(KeywordSynonym::getCanonicalTerm)
                .orElse(keyword);
    }
}
