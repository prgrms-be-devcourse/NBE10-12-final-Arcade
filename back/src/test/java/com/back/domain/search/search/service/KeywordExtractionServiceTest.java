package com.back.domain.search.search.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractionServiceTest {

    private final KeywordExtractionService extractor = new KeywordExtractionService();

    @Test
    void extractsNounKeywordsFromQuery() {
        List<String> keywords = extractor.extract("스프링 백엔드 공모전");

        assertThat(keywords).contains("스프링", "백엔드", "공모전");
    }

    @Test
    void extractsSharedKeywordsFromDifferentlyWordedTitles() {
        List<String> first = extractor.extract("백엔드 스터디원 모집합니다");
        List<String> second = extractor.extract("백엔드 개발 스터디 팀원 구해요");

        assertThat(first).contains("백엔드", "스터디");
        assertThat(second).contains("백엔드", "스터디");
    }

    @Test
    void returnsEmptyListForBlankText() {
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }
}
