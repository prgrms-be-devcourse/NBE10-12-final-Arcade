package com.back.domain.member.member.service;

import com.back.domain.member.member.dtos.MemberDto;
import com.back.domain.member.member.dtos.MemberLoginDto;
import com.back.domain.member.member.dtos.MemberLoginWithRefreshTokenDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

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

    public MemberLoginWithRefreshTokenDto login(String email, String password) {
        Member member = memberRepository.findByEmail(email).orElseThrow(
            () -> new ServiceException("401-2", "이메일 또는 비밀번호가 올바르지 않습니다.")
        );
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-2", "이메일 또는 비밀번호가 올바르지 않습니다.");

        return new MemberLoginWithRefreshTokenDto(member, genAccessToken(member));

    }

    public MemberLoginDto refreshToken(long memberId, String refreshToken) {
        Member member = memberRepository.findById(memberId).get();
        if (!member.getApiKey().equals(refreshToken))
            throw new ServiceException("401-3", "유효하지 않거나 만료된 토큰입니다.");

        return new MemberLoginDto(member, genAccessToken(member));
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

    public Map<String, Object> payload(String accessToken) {
        return authTokenService.payload(accessToken);
    }

    public Optional<Member> findById(long id) {
        return memberRepository.findById(id);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional
    public RsData<Member> modifyOrJoin(String email, String password, String name, String profileImgUrl) {
        Member member = findByEmail(email).orElse(null);

        if (member == null) {
            member = createMember(email, password, name, profileImgUrl);
            return new RsData<>("201-1", "회원가입이 완료되었습니다.", member);
        }

        modify(member, name, profileImgUrl);

        return new RsData<>("200-1", "회원 정보가 수정되었습니다.", member);
    }

    private void modify(Member member, String name, String profileImgUrl) {
        member.modify(name, profileImgUrl);
    }
}
