package com.back.domain.party.github.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.github.dtos.GithubAppUserAuthorizationUrlDto;
import com.back.domain.party.github.dtos.GithubAppUserAuthorizationStatusDto;
import com.back.domain.party.github.entity.GithubAppUserAuthorization;
import com.back.domain.party.github.entity.GithubAppUserAuthorizationState;
import com.back.domain.party.github.entity.GithubAppInstallState;
import com.back.domain.party.github.entity.GithubAppGlobalInstallState;
import com.back.domain.party.github.repository.GithubAppGlobalInstallStateRepository;
import com.back.domain.party.github.repository.GithubAppInstallStateRepository;
import com.back.domain.party.github.repository.GithubAppUserAuthorizationRepository;
import com.back.domain.party.github.repository.GithubAppUserAuthorizationStateRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/** GitHub App user-to-server OAuth 및 PKCE를 처리하고 token을 서버에 암호화 보관한다. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GithubAppUserAuthorizationService {
    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";

    private final GithubAppUserAuthorizationStateRepository stateRepository;
    private final GithubAppInstallStateRepository installStateRepository;
    private final GithubAppGlobalInstallStateRepository globalInstallStateRepository;
    private final GithubAppUserAuthorizationRepository authorizationRepository;
    private final MemberRepository memberRepository;
    private final GithubTokenCipher tokenCipher;
    private final RestClient restClient = RestClient.create();

    @Value("${custom.github.app.clientId:}")
    private String clientId;
    @Value("${custom.github.app.clientSecret:}")
    private String clientSecret;
    @Value("${custom.github.app.userAuthorizationCallbackUrl:}")
    private String callbackUrl;

    @Transactional
    public GithubAppUserAuthorizationUrlDto begin(Member actor) {
        if (actor == null) throw new ServiceException("401-1", "로그인 후 이용해주세요.");
        requireConfiguration();
        String state = random(32);
        String verifier = random(64);
        stateRepository.save(new GithubAppUserAuthorizationState(actor, state, verifier));

        String authorizationUrl = UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("state", state)
                .queryParam("code_challenge", challenge(verifier))
                .queryParam("code_challenge_method", "S256")
                .build().encode().toUriString();
        // URL에는 state가 포함되므로 URL 원문은 로그에 남기지 않는다.
        log.info("[GITHUB_APP_OAUTH_DIAG] OAuth URL issued: memberId={}", actor.getId());
        return new GithubAppUserAuthorizationUrlDto(authorizationUrl);
    }

    @Transactional
    public void complete(String state, String code) {
        if (code == null || code.isBlank()) throw new ServiceException("400-20", "GITHUB_APP_AUTHORIZATION_CODE_INVALID");
        GithubAppUserAuthorizationState authorizationState = stateRepository.findByState(state)
                .filter(GithubAppUserAuthorizationState::isUsable)
                .orElseThrow(() -> new ServiceException("400-20", "GITHUB_APP_AUTHORIZATION_STATE_INVALID"));
        saveAuthorization(authorizationState.getMember(), exchangeCode(code, authorizationState.getCodeVerifier()));
        authorizationState.consume();
    }

    /**
     * GitHub App의 "Request user authorization during installation" 흐름용이다.
     * GitHub가 App 설치 state와 installation_id를 user authorization callback으로 전달하므로,
     * 설치를 시작한 Party장의 계정에 token을 저장한다. 이 흐름은 GitHub가 시작하므로 PKCE verifier는 없다.
     */
    @Transactional
    public void completeDuringInstallation(String installState, String code) {
        if (code == null || code.isBlank()) throw new ServiceException("400-20", "GITHUB_APP_AUTHORIZATION_CODE_INVALID");
        requireConfiguration();
        GithubAppInstallState state = installStateRepository.findByState(installState)
                .filter(GithubAppInstallState::isUsable)
                .orElseThrow(() -> new ServiceException("400-20", "GITHUB_APP_INSTALL_STATE_INVALID"));
        saveAuthorization(state.getRequestedBy(), exchangeCode(code, null));
    }

    /** Party 없는 전역 설치에서 GitHub가 전달한 사용자 authorization code를 보관한다. */
    @Transactional
    public void completeDuringGlobalInstallation(String installState, String code) {
        if (code == null || code.isBlank()) throw new ServiceException("400-20", "GITHUB_APP_AUTHORIZATION_CODE_INVALID");
        requireConfiguration();
        GithubAppGlobalInstallState state = globalInstallStateRepository.findByState(installState)
                .filter(GithubAppGlobalInstallState::isUsable)
                .orElseThrow(() -> new ServiceException("400-20", "GITHUB_APP_GLOBAL_INSTALL_STATE_INVALID"));
        saveAuthorization(state.getRequestedBy(), exchangeCode(code, null));
    }

    private void saveAuthorization(Member member, TokenResponse token) {
        long githubUserId = getGithubUserId(token.accessToken());
        memberRepository.findByGithubUserId(githubUserId)
                .filter(linkedMember -> !linkedMember.getId().equals(member.getId()))
                .ifPresent(linkedMember -> {
                    throw new ServiceException("409-20", "GITHUB_APP_AUTHORIZATION_ACCOUNT_ALREADY_LINKED");
                });
        authorizationRepository.findByGithubUserId(githubUserId)
                .filter(authorization -> !authorization.getMember().getId().equals(member.getId()))
                .ifPresent(authorization -> {
                    throw new ServiceException("409-20", "GITHUB_APP_AUTHORIZATION_ACCOUNT_ALREADY_LINKED");
                });
        try {
            member.linkGithubAppUserId(githubUserId);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("403-20", "GITHUB_APP_AUTHORIZATION_ACCOUNT_MISMATCH");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessExpiresAt = now.plusSeconds(token.expiresIn());
        LocalDateTime refreshExpiresAt = token.refreshTokenExpiresIn() == null ? null : now.plusSeconds(token.refreshTokenExpiresIn());
        authorizationRepository.findByMemberId(member.getId()).ifPresentOrElse(
                existing -> existing.renew(tokenCipher.encrypt(token.accessToken()), tokenCipher.encrypt(token.refreshToken()),
                        accessExpiresAt, refreshExpiresAt),
                () -> authorizationRepository.save(new GithubAppUserAuthorization(member, githubUserId,
                        tokenCipher.encrypt(token.accessToken()), tokenCipher.encrypt(token.refreshToken()),
                        accessExpiresAt, refreshExpiresAt)));
        // access/refresh token 및 OAuth code는 로그에 남기지 않는다.
        log.info("[GITHUB_APP_OAUTH_DIAG] OAuth authorization saved: memberId={}, githubUserId={}", member.getId(), githubUserId);
    }

    /** 레포 목록·연결 검증에서만 사용하며 원문 token은 HTTP 응답으로 내보내지 않는다. */
    public String validAccessToken(Member actor) {
        if (actor == null) throw new ServiceException("401-1", "로그인 후 이용해주세요.");
        GithubAppUserAuthorization authorization = authorizationRepository.findByMemberId(actor.getId())
                .filter(GithubAppUserAuthorization::isUsable)
                .orElseThrow(() -> new ServiceException("401-20", "GITHUB_APP_USER_REAUTHORIZATION_REQUIRED"));
        return tokenCipher.decrypt(authorization.getEncryptedAccessToken());
    }

    public GithubAppUserAuthorizationStatusDto status(Member actor) {
        if (actor == null) return new GithubAppUserAuthorizationStatusDto(false, false, false, null);
        boolean socialLinked = actor.getGithubProviderUserId() != null && !actor.getGithubProviderUserId().isBlank();
        return authorizationRepository.findByMemberId(actor.getId())
                .map(authorization -> new GithubAppUserAuthorizationStatusDto(socialLinked, authorization.isUsable(),
                        !authorization.isUsable(), authorization.getAccessTokenExpiresAt()))
                .orElse(new GithubAppUserAuthorizationStatusDto(socialLinked, false, false, null));
    }

    private TokenResponse exchangeCode(String code, String codeVerifier) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", callbackUrl);
        if (codeVerifier != null && !codeVerifier.isBlank()) body.add("code_verifier", codeVerifier);
        JsonNode response = restClient.post().uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Accept", "application/json")
                .body(body).retrieve().body(JsonNode.class);
        String accessToken = response == null ? null : response.path("access_token").asString();
        long expiresIn = response == null ? 0 : response.path("expires_in").asLong();
        if (accessToken == null || accessToken.isBlank() || expiresIn <= 0) {
            throw new ServiceException("502-20", "GITHUB_APP_USER_TOKEN_CREATE_FAILED");
        }
        String refreshToken = response.path("refresh_token").asString(null);
        long refreshExpiresIn = response.path("refresh_token_expires_in").asLong();
        return new TokenResponse(accessToken, refreshToken, expiresIn, refreshExpiresIn <= 0 ? null : refreshExpiresIn);
    }

    private long getGithubUserId(String accessToken) {
        JsonNode response = restClient.get().uri(USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve().body(JsonNode.class);
        long id = response == null ? 0 : response.path("id").asLong();
        if (id <= 0) throw new ServiceException("502-20", "GITHUB_APP_USER_FETCH_FAILED");
        return id;
    }

    private void requireConfiguration() {
        if (clientId.isBlank() || clientSecret.isBlank() || callbackUrl.isBlank()) {
            throw new ServiceException("500-20", "GITHUB_APP_USER_AUTHORIZATION_NOT_CONFIGURED");
        }
    }

    private String random(int bytes) {
        byte[] value = new byte[bytes];
        new SecureRandom().nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception e) {
            throw new IllegalStateException("PKCE challenge 생성에 실패했습니다.", e);
        }
    }

    private record TokenResponse(String accessToken, String refreshToken, long expiresIn, Long refreshTokenExpiresIn) {
    }
}
