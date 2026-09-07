package com.back.domain.search.search.service.party;

import com.back.support.PostgresTestProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("prod")
@SpringBootTest
@Testcontainers
class PartySearchKeywordFtsIndexInitializerTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        PostgresTestProperties.registerForProdProfile(registry, POSTGRES);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsGinIndexOnStartup() {
        String indexDef = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'party_search_keyword_fts_idx'",
                String.class
        );

        assertThat(indexDef).containsIgnoringCase("USING gin")
                .contains("to_tsvector('simple'::regconfig, keywords)");
    }
}
