package com.back.domain.search.search.service.keyword;

import com.back.domain.search.search.entity.keyword.KeywordSynonym;
import com.back.domain.search.search.repository.keyword.KeywordSynonymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordNormalizationService implements KeywordNormalizationPort {

    private final KeywordSynonymRepository keywordSynonymRepository;

    @Override
    public List<String> normalize(List<String> keywords) {
        Map<String, String> canonicalByTerm = keywordSynonymRepository.findByTermIn(keywords).stream()
                .collect(Collectors.toMap(KeywordSynonym::getTerm, KeywordSynonym::getCanonicalTerm));

        return keywords.stream()
                .map(keyword -> canonicalByTerm.getOrDefault(keyword, keyword))
                .distinct()
                .toList();
    }
}
