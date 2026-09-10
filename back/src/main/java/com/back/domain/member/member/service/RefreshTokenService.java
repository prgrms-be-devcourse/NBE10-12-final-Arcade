package com.back.domain.member.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final Duration TTL = Duration.ofDays(14);
    private static final String TOKEN_KEY_PREFIX = "auth:refresh-token:";
    private static final String USED_TOKEN_KEY_PREFIX = "auth:used-refresh-token:";
    private static final String MEMBER_TOKEN_KEY_PREFIX = "auth:refresh-token:member:";
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            """
                    local memberId = redis.call('GET', KEYS[1])
                    if memberId then
                        redis.call('DEL', KEYS[1])
                        redis.call('SREM', ARGV[2] .. memberId, ARGV[3])
                        redis.call('SET', KEYS[2], 'used', 'EX', ARGV[1])
                        return tonumber(memberId)
                    end
                    if redis.call('EXISTS', KEYS[2]) == 1 then return -2 end
                    return -1
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public String issue(long memberId) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        redisTemplate.opsForValue().set(tokenKey(token), String.valueOf(memberId), TTL);
        redisTemplate.opsForSet().add(memberTokenKey(memberId), token);
        redisTemplate.expire(memberTokenKey(memberId), TTL);
        return token;
    }

    /** @return 회원 ID, 재사용 토큰은 -2, 유효하지 않은 토큰은 -1 */
    public Long consume(String token) {
        return redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(tokenKey(token), usedTokenKey(token)),
                String.valueOf(TTL.toSeconds()),
                MEMBER_TOKEN_KEY_PREFIX,
                token
        );
    }

    public String findMemberId(String token) {
        return redisTemplate.opsForValue().get(tokenKey(token));
    }

    public boolean isUsed(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(usedTokenKey(token)));
    }

    /** 회원에게 발급된 모든 refresh token을 즉시 제거한다. */
    public void revokeAll(long memberId) {
        String memberKey = memberTokenKey(memberId);
        Set<String> tokens = redisTemplate.opsForSet().members(memberKey);
        if (tokens != null && !tokens.isEmpty()) {
            redisTemplate.delete(tokens.stream().map(this::tokenKey).toList());
        }
        redisTemplate.delete(memberKey);
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private String usedTokenKey(String token) {
        return USED_TOKEN_KEY_PREFIX + token;
    }

    private String memberTokenKey(long memberId) {
        return MEMBER_TOKEN_KEY_PREFIX + memberId;
    }
}
