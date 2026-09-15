package com.back.domain.member.auth.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Optional<Member> findByGithubSocialIdentity(String githubProviderUserId) {
        Optional<Member> member = memberRepository.findByGithubProviderUserId(githubProviderUserId);
        if (member.isPresent()) return member;

        Long githubUserId = githubUserId(githubProviderUserId);
        return githubUserId == null ? Optional.empty() : memberRepository.findByGithubUserId(githubUserId);
    }

    @Transactional
    public RsData<Member> modifyOrJoin(String email, String password, String profileImgUrl,
            String githubProviderUserId) {

        Member member = memberRepository.findByGithubProviderUserId(githubProviderUserId).orElse(null);
        if (member == null) {
            Long githubUserId = githubUserId(githubProviderUserId);
            member = githubUserId == null ? null : memberRepository.findByGithubUserId(githubUserId).orElse(null);
        }

        if (member != null) {
            setGithubSocial(member, githubProviderUserId, email);
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

        setGithubSocial(member, githubProviderUserId, email);

        return new RsData<>("200-1", "Github 소셜 로그인 연동이 완료되었습니다.", member);
    }

    @Transactional
    public Member linkGithubSocial(Member actor, String githubProviderUserId, String githubEmail,
            String profileImgUrl) {
        // OAuth 요청에서 전달받은 actor는 이 트랜잭션에서는 detached 상태일 수 있으므로,
        // 변경 감지가 가능한 managed 엔티티를 다시 조회한다.
        Member managedActor = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));
        Member linkedMember = memberRepository.findByGithubProviderUserId(githubProviderUserId).orElse(null);

        if (linkedMember != null && !Objects.equals(linkedMember.getId(), managedActor.getId())) {
            throw new ServiceException("409-1", "이미 다른 계정에 연결된 GitHub 계정입니다.");
        }
        Long githubUserId = githubUserId(githubProviderUserId);
        Member appLinkedMember = githubUserId == null ? null : memberRepository.findByGithubUserId(githubUserId).orElse(null);
        if (appLinkedMember != null && !Objects.equals(appLinkedMember.getId(), managedActor.getId())) {
            throw new ServiceException("409-1", "이미 다른 계정에 GitHub App 인증으로 연결된 GitHub 계정입니다.");
        }

        if (managedActor.getGithubProviderUserId() != null && !managedActor.getGithubProviderUserId().isBlank()) {
            throw new ServiceException("400-1", "현재 계정에는 이미 GitHub 계정이 연결되어 있습니다.");
        }

        setGithubSocial(managedActor, githubProviderUserId, githubEmail);
        managedActor.setProfileImgUrl(profileImgUrl);

        return managedActor;
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

    private void setGithubSocial(Member member, String githubProviderUserId, String githubEmail) {
        try {
            member.setGithubSocial(githubProviderUserId, githubEmail);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("403-1", "GITHUB_SOCIAL_ACCOUNT_MISMATCH");
        }
    }

    private Long githubUserId(String githubProviderUserId) {
        if (githubProviderUserId == null) return null;
        String value = githubProviderUserId.startsWith("GITHUB__")
                ? githubProviderUserId.substring("GITHUB__".length()) : githubProviderUserId;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
