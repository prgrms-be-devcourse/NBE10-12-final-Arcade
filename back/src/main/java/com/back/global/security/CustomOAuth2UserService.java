package com.back.global.security;

import com.back.domain.member.auth.service.AuthService;
import com.back.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AuthService authService;

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
                email = (String) attributes.get("email");
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
}
