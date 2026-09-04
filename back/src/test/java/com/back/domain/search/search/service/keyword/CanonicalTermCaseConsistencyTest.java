package com.back.domain.search.search.service.keyword;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CanonicalTermCaseConsistencyTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private KeywordExtractionPort keywordExtractionPort;

    @Autowired
    private KeywordNormalizationPort keywordNormalizationPort;

    @Autowired
    private RelatedTermExpansionPort relatedTermExpansionPort;

    @Test
    void aiSynonymExpandsToRelatedTermsOnRealPostgres() {
        List<String> extracted = keywordExtractionPort.extract("인공지능 개발자");
        List<String> normalized = keywordNormalizationPort.normalize(extracted);
        List<String> expanded = relatedTermExpansionPort.expand(normalized);

        assertThat(normalized).contains("ai");
        assertThat(expanded).contains("머신러닝");
    }
}
