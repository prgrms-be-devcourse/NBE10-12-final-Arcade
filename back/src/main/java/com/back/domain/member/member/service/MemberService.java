package com.back.domain.member.member.service;

import com.back.domain.member.member.dtos.MemberDto;
import com.back.domain.member.member.dtos.MemberLoginDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh-token:";
    private static final String USED_REFRESH_TOKEN_KEY_PREFIX = "auth:used-refresh-token:";
    private static final DefaultRedisScript<Long> CONSUME_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>(
        """
            local memberId = redis.call('GET', KEYS[1])
            if memberId then
                redis.call('DEL', KEYS[1])
                redis.call('SET', KEYS[2], 'used', 'EX', ARGV[1])
                return tonumber(memberId)
            end
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return -2
            end
            return -1
            """,
        Long.class
    );

    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${custom.accessToken.expirationSeconds}")
    private int accessTokenExpirationSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public long count() {
        return memberRepository.count();
    }

    public MemberDto join(String email, String password, String name) {
        return join(email, password, name, null);
    }

    @Transactional
    public MemberDto join(String email, String password, String name, String profileImgUrl) {
        findByEmail(email)
                .ifPresent(_ -> {
                    throw new ServiceException("409-1", "이미 사용 중인 이메일입니다.");
                });

        return new MemberDto(createMember(email, password, name, profileImgUrl));
    }

    /**
     * 외부 호출자는 엔티티 대신 DTO를 받도록 하되, 서비스 내부 유스케이스는
     * 같은 트랜잭션 안에서 영속 엔티티를 계속 다룰 수 있게 한다.
     */
    private Member createMember(String email, String password, String name, String profileImgUrl) {
        String encodedPassword = (password != null && !password.isBlank())
                ? passwordEncoder.encode(password)
                : null;

        return memberRepository.save(new Member(email, encodedPassword, name, profileImgUrl));
    }

    @Transactional
    public void modifyApiKey(long memberId, String apiKey) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        member.modifyApiKey(apiKey);
    }

    public MemberLoginDto login(String email, String password) {
        Member member = memberRepository.findByEmail(email).orElseThrow(
            () -> new ServiceException("401-2", "이메일 또는 비밀번호가 올바르지 않습니다.")
        );
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-2", "이메일 또는 비밀번호가 올바르지 않습니다.");

        return createLoginDto(member);

    }

    public MemberLoginDto refreshToken(String refreshToken) {
        Long memberId = redisTemplate.execute(
            CONSUME_REFRESH_TOKEN_SCRIPT,
            List.of(refreshTokenKey(refreshToken), usedRefreshTokenKey(refreshToken)),
            String.valueOf(REFRESH_TOKEN_TTL.toSeconds())
        );

        if (Long.valueOf(-2).equals(memberId)) {
            throw new ServiceException("401-4", "재사용된 리프레시 토큰입니다.");
        }

        if (memberId == null || memberId < 1) {
            throw new ServiceException("401-3", "유효하지 않거나 만료된 토큰입니다.");
        }

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        return createLoginDto(member);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Optional<Member> findByApiKey(String apiKey) {
        return memberRepository.findByApiKey(apiKey);
    }

    public String genAccessToken(Member member) {
        return authTokenService.genAccessToken(member);
    }

    private MemberLoginDto createLoginDto(Member member) {
        String refreshToken = generateRefreshToken();
        redisTemplate.opsForValue().set(
            refreshTokenKey(refreshToken),
            String.valueOf(member.getId()),
            REFRESH_TOKEN_TTL
        );

        return new MemberLoginDto(
            member,
            genAccessToken(member),
            refreshToken,
            accessTokenExpirationSeconds
        );
    }

    private String generateRefreshToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String refreshTokenKey(String refreshToken) {
        return REFRESH_TOKEN_KEY_PREFIX + refreshToken;
    }

    private String usedRefreshTokenKey(String refreshToken) {
        return USED_REFRESH_TOKEN_KEY_PREFIX + refreshToken;
    }

    public AuthTokenService.AccessTokenPayload payload(String accessToken) {
        return authTokenService.payload(accessToken);
    }

    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

}
