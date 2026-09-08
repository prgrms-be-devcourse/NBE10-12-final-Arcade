package com.back.domain.search.search.service.party;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class PartySearchKeywordFtsIndexInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS party_search_keyword_fts_idx
                    ON party_search_keyword
                    USING GIN (to_tsvector('simple', keywords))
                    """);
            log.info("party_search_keyword FTS GIN 인덱스 확인 완료");
        } catch (Exception e) {
            log.warn("party_search_keyword FTS GIN 인덱스 생성에 실패했습니다. 인덱스 없이도 검색은 동작합니다.", e);
        }
    }
}
