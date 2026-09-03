package com.back.domain.search.search.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.search.search.repository.SearchLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SearchLogServiceReadOnlyTest {

    @Autowired
    private SearchLogService searchLogService;

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void logSucceedsInsideReadOnlyTransaction() {
        Member actor = memberRepository.save(new Member("readonly-log-test@test.com", "pw", "tester", null));

        TransactionTemplate readOnlyTemplate = new TransactionTemplate(transactionManager);
        readOnlyTemplate.setReadOnly(true);
        readOnlyTemplate.executeWithoutResult(status ->
                searchLogService.log(actor, "리드온리 트랜잭션 테스트")
        );

        assertThat(searchLogRepository.findAll())
                .anyMatch(log -> log.getKeyword().equals("리드온리 트랜잭션 테스트"));
    }
}
