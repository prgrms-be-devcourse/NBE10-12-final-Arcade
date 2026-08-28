package com.back.global.security;

import com.back.domain.member.auth.service.AuthService;
import com.back.domain.member.member.entity.Member;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final String GITHUB_EMAILS_API_URL = "https://api.github.com/user/emails";

    private final AuthService authService;
    private final RestClient restClient = RestClient.create();

    // 카카오톡 로그인이 성공할 때 마다 이 함수가 실행된다.
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String oauthUserId = "";
        String providerTypeCode = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        String email = "";
        String profileImgUrl = "";

        switch (providerTypeCode) {
            case "GITHUB" -> {
                Map<String, Object> attributes = oAuth2User.getAttributes();

                oauthUserId = oAuth2User.getName();
                email = getGithubVerifiedEmail(userRequest);
                profileImgUrl = (String) attributes.get("avatar_url");

            }
        }

        String username = providerTypeCode + "__%s".formatted(oauthUserId);
        String password = "";
        Member member = authService.modifyOrJoin(email, password, profileImgUrl, username).data();

        return new SecurityUser(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getAuthorities()
        );
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
