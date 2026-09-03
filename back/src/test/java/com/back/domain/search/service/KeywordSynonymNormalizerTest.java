package com.back.domain.search.service;

import com.back.domain.search.search.service.KeywordNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KeywordSynonymNormalizerTest {

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Test
    void normalizesKnownSynonymsToCanonicalForm() {
        List<String> normalized = keywordNormalizer.normalize(List.of("서버", "리액트"));

        assertThat(normalized).contains("백엔드", "리액트");
    }

    @Test
    void leavesUnknownTermsUnchanged() {
        List<String> normalized = keywordNormalizer.normalize(List.of("크루온"));

        assertThat(normalized).containsExactly("크루온");
    }

    @Test
    void deduplicatesAfterNormalization() {
        List<String> normalized = keywordNormalizer.normalize(List.of("서버", "backend"));

        assertThat(normalized).containsExactly("백엔드");
    }
}
