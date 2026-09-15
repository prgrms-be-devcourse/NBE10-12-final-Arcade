package com.back.domain.member.member.service;

import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PasswordResetRequestRateLimiter {
    private static final String KEY_PREFIX = "auth:password-reset:rate:";

    private final StringRedisTemplate redisTemplate;
    private final CustomConfigProperties customConfigProperties;

    public void check(String normalizedEmail, String clientIp) {
        CustomConfigProperties.RateLimit.PasswordResetRequest config = customConfigProperties
                .getRateLimit().getPasswordResetRequest();
        Duration window = Duration.ofSeconds(config.getWindowSeconds());

        if (!incrementWithinLimit(KEY_PREFIX + "email:" + sha256(normalizedEmail), config.getEmailMaxRequests(), window)
                || !incrementWithinLimit(KEY_PREFIX + "ip:" + sha256(clientIp), config.getIpMaxRequests(), window)) {
            throw new ServiceException("429-1", "잠시 후 다시 시도해주세요.");
        }
    }

    private boolean incrementWithinLimit(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1).equals(count)) redisTemplate.expire(key, window);
        return count != null && count <= limit;
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
