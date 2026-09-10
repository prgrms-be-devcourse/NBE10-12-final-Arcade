package com.back.domain.member.member.service;

import com.back.domain.member.member.dtos.MemberDetailDto;
import com.back.domain.member.member.dtos.MemberDto;
import com.back.domain.member.member.dtos.MemberListItemDto;
import com.back.domain.member.member.dtos.MemberLoginDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;
    private final MemberProfileRepository memberProfileRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${custom.accessToken.expirationSeconds}")
    private int accessTokenExpirationSeconds;

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

        if (!member.isActive())
            throw new ServiceException("403-2", "정지된 계정입니다. 사유: " + member.getSuspendReason());

        return createLoginDto(member);

    }

    public MemberLoginDto refreshToken(String refreshToken) {
        Long memberId = refreshTokenService.consume(refreshToken);

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
        String memberIdValue = refreshTokenService.findMemberId(refreshToken);

        if (memberIdValue == null) {
            if (refreshTokenService.isUsed(refreshToken)) {
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
        String refreshToken = refreshTokenService.issue(member.getId());

        return new MemberLoginDto(
            member,
            genAccessToken(member),
            refreshToken,
            accessTokenExpirationSeconds
        );
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

    public Page<MemberListItemDto> getListForAdmin(String keyword, Role role, Boolean active, Pageable pageable) {
        Page<Member> members = memberRepository.searchForAdmin(keyword, role, active, pageable);

        Map<Long, MemberProfile> profilesByMemberId = memberProfileRepository
                .findByMember_IdIn(members.getContent().stream().map(Member::getId).toList())
                .stream()
                .collect(Collectors.toMap(profile -> profile.getMember().getId(), profile -> profile));

        return members.map(member -> new MemberListItemDto(member, profilesByMemberId.get(member.getId())));
    }

    public MemberDetailDto getDetailForAdmin(long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        MemberProfile profile = memberProfileRepository.findByMember(member).orElse(null);

        return new MemberDetailDto(member, profile);
    }

    @Transactional
    public void updateStatus(long memberId, boolean active, String reason) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        if (!active && member.isAdmin()) {
            throw new ServiceException("409-2", "관리자 계정은 정지할 수 없습니다.");
        }

        if (active) {
            member.activate();
        } else {
            member.suspend(reason);
        }
    }
}
