package com.back.domain.search.search.service.keyword;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KeywordNormalizationServiceTest {

    @Autowired
    private KeywordNormalizationPort keywordNormalizationPort;

    @Test
    void normalizesKnownSynonymsToCanonicalForm() {
        List<String> normalized = keywordNormalizationPort.normalize(List.of("frontend", "리액트"));

        assertThat(normalized).contains("프론트엔드", "리액트");
    }

    @Test
    void leavesUnknownTermsUnchanged() {
        List<String> normalized = keywordNormalizationPort.normalize(List.of("크루온"));

        assertThat(normalized).containsExactly("크루온");
    }
}
