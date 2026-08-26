package com.back.domain.member.member.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    public long count() {
        return memberRepository.count();
    }

    public Member join(String username, String password, String nickname) {
        return join(username, password, nickname, null);
    }

    public Member join(String username, String password, String nickname, String profileImgUrl) {
        findByUsername(username)
                .ifPresent(_ -> {
                    throw new ServiceException("409-1", "이미 존재하는 아이디입니다.");
                });

        password = (password != null && !password.isBlank()) ? passwordEncoder.encode(password) : null;

        Member member = new Member(username, password, nickname, profileImgUrl);

        return memberRepository.save(member);
    }

    public void checkPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword()))
            throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");
    }

    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
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

    public Optional<Member> findById(int id) {
        return memberRepository.findById(id);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public RsData<Member> modifyOrJoin(String username, String password, String nickname, String profileImgUrl) {
        Member member = findByUsername(username).orElse(null);

        if (member == null) {
            member = join(username, password, nickname, profileImgUrl);
            return new RsData<>("201-1", "회원가입이 완료되었습니다.", member);
        }

        modify(member, nickname, profileImgUrl);

        return new RsData<>("200-1", "회원 정보가 수정되었습니다.", member);
    }

    private void modify(Member member, String nickname, String profileImgUrl) {
        member.modify(nickname, profileImgUrl);
    }
}
