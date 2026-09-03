package com.back.domain.search.search.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.search.search.repository.SearchLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SearchLogServiceReadOnlyPostgresTest {

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
    private SearchLogService searchLogService;

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void logSucceedsInsideReadOnlyTransactionOnRealPostgres() {
        Member actor = memberRepository.save(new Member("readonly-log-pg-test@test.com", "pw", "tester", null));

        TransactionTemplate readOnlyTemplate = new TransactionTemplate(transactionManager);
        readOnlyTemplate.setReadOnly(true);
        readOnlyTemplate.executeWithoutResult(status ->
                searchLogService.log(actor, "포스트그레스 리드온리 테스트")
        );

        assertThat(searchLogRepository.findAll())
                .anyMatch(log -> log.getKeyword().equals("포스트그레스 리드온리 테스트"));
    }
}
