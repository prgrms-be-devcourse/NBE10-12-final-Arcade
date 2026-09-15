package com.back.domain.search.search.service.keyword;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractionSpecialCharTest {

    private final KeywordExtractionService extractor = new KeywordExtractionService();

    @Test
    void extractsFromTextContainingTsqueryOperatorCharacters() {
        List<String> keywords = extractor.extract("AT&T 개발자 모집 (급구!) 백엔드/프론트엔드");

        assertThat(keywords).noneMatch(k -> k.contains("&") || k.contains("(") || k.contains(")")
                || k.contains("!") || k.contains("|") || k.contains(":"));
    }

    @Test
    void extractsFromTextContainingAngleBracketsAndColons() {
        List<String> keywords = extractor.extract("<script>alert(1)</script> 회원:일반 등급 파티");

        assertThat(keywords).noneMatch(k -> k.contains("<") || k.contains(">") || k.contains(":"));
    }

    @Test
    void extractedKeywordsNeverContainEmbeddedWhitespace() {
        List<String> keywords = extractor.extract("노드 js 스터디 R&D 부서");

        assertThat(keywords).noneMatch(k -> k.contains(" "));
    }
}
