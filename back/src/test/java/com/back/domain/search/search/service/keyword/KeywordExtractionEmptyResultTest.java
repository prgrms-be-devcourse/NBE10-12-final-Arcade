package com.back.domain.search.search.service.keyword;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractionEmptyResultTest {

    private final KeywordExtractionService extractor = new KeywordExtractionService();

    @Test
    void returnsEmptyForParticlesAndConjunctionsOnly() {
        List<String> keywords = extractor.extract("그리고 그래서");

        System.out.println("EXTRACTED: " + keywords);
        assertThat(keywords).isEmpty();
    }

    @Test
    void returnsEmptyForPunctuationOnly() {
        List<String> keywords = extractor.extract("!?@#");

        System.out.println("EXTRACTED: " + keywords);
        assertThat(keywords).isEmpty();
    }
}
