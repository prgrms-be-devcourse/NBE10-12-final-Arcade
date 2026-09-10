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
import java.time.LocalDateTime;
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

    /**
     * 동의 없이 회원만 만든다. 시드·개발 데이터가 쓴다.
     *
     * 시드 관리자는 이어지는 grantAdmin() 이 동의를 함께 기록한다(BaseInitData).
     */
    @Transactional
    public MemberDto join(String email, String password, String name) {
        return join(email, password, name, false);
    }

    /**
     * 가입 폼 경로. 필수 약관 동의를 **가입과 같은 트랜잭션에** 기록한다 -
     * 따로 부르면 그 사이에 실패해 "가입은 됐는데 동의 기록이 없는" 계정이 남는다.
     *
     * 두 오버로드 모두 @Transactional 이 필요하다. 여기가 호출자가 지나가는 프록시 경계라,
     * 쓰기 트랜잭션을 열지 않으면 클래스 레벨 readOnly=true 가 유지된다.
     * readOnly 면 flush 를 하지 않아 INSERT 는 되는데(IDENTITY 라 즉시 필요)
     * 그 뒤에 바꾼 필드가 조용히 사라진다.
     */
    @Transactional
    public MemberDto join(String email, String password, String name, boolean agreedToRequiredTerms) {
        findByEmail(email)
                .ifPresent(_ -> {
                    throw new ServiceException("409-1", "이미 사용 중인 이메일입니다.");
                });

        Member member = createMember(email, password, name);

        if (agreedToRequiredTerms) member.agreeToRequiredTerms(LocalDateTime.now());

        return new MemberDto(member);
    }

    /**
     * 필수 약관 동의를 기록한다. GitHub 가입자는 폼이 없어 가입 이후에 이 경로로 동의한다.
     *
     * 프로필 수정(PATCH /members/me)과 분리한 이유는, 그쪽이 언제든 불리는 API 라
     * 섞으면 **닉네임을 고칠 때마다 동의 일시가 갱신**되기 때문이다.
     */
    @Transactional
    public void agreeToRequiredTerms(Member actor) {
        Member member = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        member.agreeToRequiredTerms(LocalDateTime.now());
    }

    /**
     * 외부 호출자는 엔티티 대신 DTO를 받도록 하되, 서비스 내부 유스케이스는
     * 같은 트랜잭션 안에서 영속 엔티티를 계속 다룰 수 있게 한다.
     */
    private Member createMember(String email, String password, String name) {
        String encodedPassword = (password != null && !password.isBlank())
                ? passwordEncoder.encode(password)
                : null;

        // 소셜 가입은 이 경로를 타지 않는다(AuthService.modifyOrJoin 이 따로 만든다).
        // 그래서 프로필 이미지는 항상 비어 있다 - 예전엔 받아 두었지만 아무도 넘기지 않았다.
        return memberRepository.save(new Member(email, encodedPassword, name, null));
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

    /**
     * refresh token을 소비하지 않고 유효성만 확인한다.
     *
     * access token 자동 갱신 경로에서 refresh token을 rotation하면 같은 쿠키로 들어온
     * 동시 요청이 서로를 무효화할 수 있으므로, 이 메서드는 access token 재발급에만 사용한다.
     */
    public Member getMemberByRefreshToken(String refreshToken) {
        String memberIdValue = redisTemplate.opsForValue().get(refreshTokenKey(refreshToken));

        if (memberIdValue == null) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(usedRefreshTokenKey(refreshToken)))) {
                throw new ServiceException("401-4", "재사용된 리프레시 토큰입니다.");
            }

            throw new ServiceException("401-3", "유효하지 않거나 만료된 토큰입니다.");
        }

        try {
            long memberId = Long.parseLong(memberIdValue);
            return memberRepository.findById(memberId)
                    .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));
        } catch (NumberFormatException e) {
            throw new ServiceException("401-3", "유효하지 않거나 만료된 토큰입니다.");
        }
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

    /**
     * 비밀번호 인증 또는 OAuth 인증이 완료된 회원에게 새 토큰 쌍을 발급한다.
     */
    public MemberLoginDto createLoginDto(Member member) {
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
