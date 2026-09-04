package com.back.domain.search.search.service.keyword;

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
    void returnsEmptyListForBlankText() {
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }

    @Test
    void excludesBoundNounsLikeCountUnits() {
        List<String> keywords = extractor.extract("팀원 3명 구하는 프로젝트");

        assertThat(keywords).doesNotContain("명");
        assertThat(keywords).contains("팀원", "프로젝트");
    }
}
