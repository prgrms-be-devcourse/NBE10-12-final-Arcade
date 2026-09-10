package com.back.domain.member.member.service;

import com.back.RedisTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisTestContainerConfig.class)
class PasswordResetTokenStoreTest {
    @Autowired private PasswordResetTokenStore tokenStore;
    @Autowired private StringRedisTemplate redisTemplate;

    @Test
    void storesOnlyTokenHashWithTtlAndInvalidatesPreviousToken() {
        long memberId = System.nanoTime();
        String firstToken = tokenStore.issue(memberId);
        String secondToken = tokenStore.issue(memberId);

        assertThat(tokenStore.isValid(firstToken)).isFalse();
        assertThat(tokenStore.isValid(secondToken)).isTrue();
        assertThat(redisTemplate.keys("auth:password-reset:token:*")
                .stream().noneMatch(key -> key.contains(secondToken))).isTrue();
        assertThat(redisTemplate.getExpire("auth:password-reset:member:" + memberId)).isPositive();
    }

    @Test
    void consumesSameTokenOnlyOnceUnderConcurrentRequests() throws Exception {
        long memberId = System.nanoTime();
        String token = tokenStore.issue(memberId);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Long>> requests = List.of(
                    () -> tokenStore.consume(token),
                    () -> tokenStore.consume(token)
            );

            List<Long> results = executor.invokeAll(requests).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            assertThat(results).containsExactlyInAnyOrder(memberId, null);
        } finally {
            executor.shutdownNow();
        }
    }
}
