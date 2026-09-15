package com.back.domain.member.profile.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.support.PostgresTestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class MemberProfileServiceConcurrencyTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        PostgresTestProperties.register(registry, POSTGRES);
    }

    @Autowired
    private MemberProfileService memberProfileService;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private MemberRepository memberRepository;

    // 마이페이지가 열리면 조회와 자동저장이 거의 동시에 들어와 둘 다 프로필을 처음 만들려 한다.
    @Test
    @DisplayName("프로필 없는 회원의 동시 조회는 실패 없이 한 행만 만든다")
    void concurrentFirstProfileCreationDoesNotThrow() throws InterruptedException {
        Member member = memberRepository.save(
                new Member("profile-concurrency@test.com", "pw", "동시성", null));

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    memberProfileService.me(member);
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(failures.get()).isZero();
        assertThat(memberProfileRepository.findByMember_IdIn(List.of(member.getId()))).hasSize(1);
    }
}
