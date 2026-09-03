package com.back.domain.search.search.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RelatedTermExpansionServiceTest {

    @Autowired
    private RelatedTermExpansionPort relatedTermExpansionPort;

    @Test
    void expandsToRelatedTermsInTheSameGroup() {
        List<String> expanded = relatedTermExpansionPort.expand(List.of("서버"));

        assertThat(expanded).contains("서버", "백엔드", "devops", "인프라");
    }

    @Test
    void doesNotCrossOverToUnrelatedGroups() {
        List<String> expanded = relatedTermExpansionPort.expand(List.of("서버"));

        assertThat(expanded).doesNotContain("디자인", "기획", "클라이언트");
    }

    @Test
    void keepsOriginalKeywordWhenNoRelationExists() {
        List<String> expanded = relatedTermExpansionPort.expand(List.of("리액트"));

        assertThat(expanded).containsExactly("리액트");
    }
}
