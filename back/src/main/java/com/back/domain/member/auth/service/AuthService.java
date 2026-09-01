package com.back.domain.member.auth.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Transactional
    public RsData<Member> modifyOrJoin(String email, String password, String profileImgUrl,
            String githubProviderUserId) {

        Member member = memberRepository.findByGithubProviderUserId(githubProviderUserId).orElse(null);

        if (member != null) {

            modify(member, profileImgUrl);
            return new RsData<>("200-1", "회원 정보가 수정되었습니다.", member);

        }

        if (email != null && !email.isBlank()) {
            member = memberRepository.findByEmail(email).orElse(null);
        }

        if (member == null) {
            member = createMember(email, password, profileImgUrl, githubProviderUserId);
            return new RsData<>("201-1", "회원가입이 완료되었습니다.", member);
        }

        member.setGithubSocial(githubProviderUserId, email);

        return new RsData<>("200-1", "Github 소셜 로그인 연동이 완료되었습니다.", member);
    }

    @Transactional
    public Member linkGithubSocial(Member actor, String githubProviderUserId, String githubEmail,
            String profileImgUrl) {
        Member linkedMember = memberRepository.findByGithubProviderUserId(githubProviderUserId).orElse(null);

        if (linkedMember != null && linkedMember.getId() != actor.getId()) {
            throw new ServiceException("409-1", "이미 다른 계정에 연결된 GitHub 계정입니다.");
        }

        if (actor.getGithubProviderUserId() != null && !actor.getGithubProviderUserId().isBlank()) {
            throw new ServiceException("400-1", "현재 계정에는 이미 GitHub 계정이 연결되어 있습니다.");
        }

        actor.setGithubSocial(githubProviderUserId, githubEmail);
        actor.setProfileImgUrl(profileImgUrl);

        return actor;
    }

    private void modify(Member member, String profileImgUrl) {
        member.setProfileImgUrl(profileImgUrl);
    }

    private Member createMember(String email, String password, String profileImgUrl,
            String githubProviderUserId) {
        String encodedPassword = (password != null && !password.isBlank())
                ? passwordEncoder.encode(password)
                : null;

        Member newMember = new Member(email, encodedPassword, null, profileImgUrl);
        newMember.setGithubSocial(githubProviderUserId, email);

        return memberRepository.save(newMember);
    }
}
