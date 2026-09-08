package com.back.global.security;

import com.back.domain.member.auth.service.AuthService;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final String GITHUB_EMAILS_API_URL = "https://api.github.com/user/emails";

    private final AuthService authService;
    private final Rq rq;
    private final RestClient restClient = RestClient.create();

    // 카카오톡 로그인이 성공할 때 마다 이 함수가 실행된다.
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String oauthUserId = "";
        String providerTypeCode = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        String profileImgUrl = "";

        switch (providerTypeCode) {
            case "GITHUB" -> {
                Map<String, Object> attributes = oAuth2User.getAttributes();

                oauthUserId = oAuth2User.getName();
                profileImgUrl = (String) attributes.get("avatar_url");

            }
        }

        String username = providerTypeCode + "__%s".formatted(oauthUserId);
        Member actor = rq.getActor();
        Member member;

        if (actor == null) {
            Optional<Member> existingMember = authService.findByGithubSocialIdentity(username);

            if (existingMember.isPresent()) {
                // 이미 GitHub ID가 연결된 회원은 이메일이 회원 식별에 필요하지 않다.
                member = existingMember.get();
            } else {
                // 최초 소셜 로그인에서만 검증된 이메일로 신규 가입 또는 기존 이메일 계정 연결을 판단한다.
                String email = getGithubVerifiedEmail(userRequest);
                member = authService.modifyOrJoin(email, "", profileImgUrl, username).data();
            }
        } else {
            try {
                // 최초 계정 연결에서는 검증된 GitHub 이메일도 함께 저장한다.
                String email = getGithubVerifiedEmail(userRequest);
                member = authService.linkGithubSocial(actor, username, email, profileImgUrl);
            } catch (ServiceException e) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(e.getRsData().resultCode()),
                        e.getRsData().msg()
                );
            }
        }

        return new SecurityUser(member.getId(), member.getRole());
    }

    private String getGithubVerifiedEmail(OAuth2UserRequest userRequest) {
        GithubEmail[] emails;

        try {
            emails = restClient.get()
                    .uri(GITHUB_EMAILS_API_URL)
                    .headers(headers -> {
                        headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
                        headers.set("Accept", "application/vnd.github+json");
                    })
                    .retrieve()
                    .body(GithubEmail[].class);
        } catch (RestClientException e) {
            log.warn("GitHub 이메일 API 호출에 실패했습니다.", e);
            throw githubEmailNotAvailable();
        }

        if (emails != null) {
            for (GithubEmail githubEmail : emails) {
                if (githubEmail.primary() && githubEmail.verified()) {
                    return githubEmail.email();
                }
            }

            for (GithubEmail githubEmail : emails) {
                if (githubEmail.verified()) {
                    return githubEmail.email();
                }
            }
        }

        throw githubEmailNotAvailable();
    }

    private OAuth2AuthenticationException githubEmailNotAvailable() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("github_email_not_available"),
                "GitHub 계정의 검증된 이메일을 확인할 수 없습니다."
        );
    }

    private record GithubEmail(String email, boolean primary, boolean verified) {
    }
}
