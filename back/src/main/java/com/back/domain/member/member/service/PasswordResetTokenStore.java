package com.back.domain.member.member.service;

import com.back.global.app.CustomConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenStore {
    private static final String TOKEN_KEY_PREFIX = "auth:password-reset:token:";
    private static final String MEMBER_KEY_PREFIX = "auth:password-reset:member:";
    private static final DefaultRedisScript<Long> REPLACE_TOKEN_SCRIPT = new DefaultRedisScript<>(
            """
                    local oldHash = redis.call('GET', KEYS[2])
                    if oldHash then redis.call('DEL', ARGV[1] .. oldHash) end
                    redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
                    redis.call('SET', KEYS[2], ARGV[4], 'EX', ARGV[3])
                    return 1
                    """,
            Long.class
    );
    private static final DefaultRedisScript<Long> CONSUME_TOKEN_SCRIPT = new DefaultRedisScript<>(
            """
                    local memberId = redis.call('GET', KEYS[1])
                    if not memberId then return -1 end
                    if redis.call('GET', KEYS[2]) ~= ARGV[1] then return -1 end
                    redis.call('DEL', KEYS[1])
                    redis.call('DEL', KEYS[2])
                    return tonumber(memberId)
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final CustomConfigProperties customConfigProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(long memberId) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = sha256(token);
        Duration ttl = Duration.ofSeconds(customConfigProperties.getPasswordReset().getExpirationSeconds());

        redisTemplate.execute(
                REPLACE_TOKEN_SCRIPT,
                List.of(tokenKey(tokenHash), memberKey(memberId)),
                TOKEN_KEY_PREFIX,
                String.valueOf(memberId),
                String.valueOf(ttl.toSeconds()),
                tokenHash
        );
        return token;
    }

    public boolean isValid(String token) {
        if (!isTokenFormatValid(token)) return false;
        String tokenHash = sha256(token);
        String memberId = redisTemplate.opsForValue().get(tokenKey(tokenHash));
        return memberId != null && tokenHash.equals(redisTemplate.opsForValue().get(memberKey(memberId)));
    }

    /** 같은 토큰의 동시 제출 중 하나만 회원 ID를 반환하고 나머지는 빈 결과를 반환한다. */
    public Long consume(String token) {
        if (!isTokenFormatValid(token)) return null;
        String tokenHash = sha256(token);
        String memberId = redisTemplate.opsForValue().get(tokenKey(tokenHash));
        if (memberId == null) return null;

        Long consumedMemberId = redisTemplate.execute(
                CONSUME_TOKEN_SCRIPT,
                List.of(tokenKey(tokenHash), memberKey(memberId)),
                tokenHash
        );
        return consumedMemberId != null && consumedMemberId > 0 ? consumedMemberId : null;
    }

    private boolean isTokenFormatValid(String token) {
        return token != null && token.matches("^[A-Za-z0-9_-]{43}$");
    }

    private String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private String memberKey(long memberId) {
        return MEMBER_KEY_PREFIX + memberId;
    }

    private String memberKey(String memberId) {
        return MEMBER_KEY_PREFIX + memberId;
    }

    private String sha256(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
