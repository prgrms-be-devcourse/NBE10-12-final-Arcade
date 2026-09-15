package com.back.domain.party.github.controller;

import com.back.domain.party.github.dtos.GithubAppUserAuthorizationUrlDto;
import com.back.domain.party.github.dtos.GithubAppUserAuthorizationStatusDto;
import com.back.domain.party.github.service.GithubAppUserAuthorizationService;
import com.back.domain.party.github.service.PartyGithubConnectionService;
import com.back.domain.party.github.service.GithubInstallationInventoryService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** GitHub App 사용자 인증은 소셜 로그인과 분리해, 레포 연결 권한 확인에만 사용한다. */
@RestController
@RequestMapping("/api/v1/github-app/user")
@RequiredArgsConstructor
@Slf4j
public class ApiV1GithubAppUserAuthorizationController {
    private final GithubAppUserAuthorizationService authorizationService;
    private final PartyGithubConnectionService githubConnectionService;
    private final GithubInstallationInventoryService inventoryService;
    private final Rq rq;

    @Value("${custom.frontend.base-url:/}")
    private String frontendBaseUrl;

    @PostMapping("/authorize")
    public RsData<GithubAppUserAuthorizationUrlDto> authorize() {
        var actor = rq.getActorFromDb();
        log.info("[GITHUB_APP_OAUTH_DIAG] OAuth authorize endpoint called: memberId={}", actor == null ? null : actor.getId());
        return new RsData<>("200-1", "GitHub App 사용자 인증 URL 생성 성공",
                authorizationService.begin(actor));
    }

    @GetMapping("/authorization-status")
    public RsData<GithubAppUserAuthorizationStatusDto> authorizationStatus() {
        return new RsData<>("200-1", "GitHub App 사용자 인증 상태 조회 성공",
                authorizationService.status(rq.getActorFromDb()));
    }

    /** GitHub의 일반 user authorization 및 설치 중 자동 user authorization callback을 함께 처리한다. */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(name = "installation_id", required = false) Long installationId
    ) {
        // state/code 원문은 민감값이므로 존재 여부만 기록한다.
        log.info("[GITHUB_APP_OAUTH_DIAG] OAuth callback received: hasState={}, hasCode={}, installationId={}",
                state != null && !state.isBlank(), code != null && !code.isBlank(), installationId);
        if (installationId != null && installationId > 0) {
            // "Request user authorization during installation"은 Setup URL 대신 이 callback을 호출한다.
            if (state != null && !state.isBlank()) {
                if (githubConnectionService.isGlobalInstallState(state)) {
                    authorizationService.completeDuringGlobalInstallation(state, code);
                    githubConnectionService.completeGlobalInstall(state, installationId);
                    log.info("GitHub App global installation callback completed: installationId={}", installationId);
                    return ResponseEntity.status(HttpStatus.FOUND).location(installedRedirectUri()).build();
                }
                authorizationService.completeDuringInstallation(state, code);
                PartyGithubConnectionService.InstallCompletion completion = githubConnectionService.completeInstall(state, installationId);
                log.info("GitHub App Party installation callback completed: partyId={}, installationId={}", completion.partyId(), installationId);
                return ResponseEntity.status(HttpStatus.FOUND).location(installationRedirectUri(completion)).build();
            }
            // Party와 무관한 전역 설치는 설치 inventory만 동기화하고 binding을 만들지 않는다.
            inventoryService.syncInstallation(installationId);
            log.info("GitHub App direct installation callback completed: installationId={}", installationId);
            return ResponseEntity.status(HttpStatus.FOUND).location(installedRedirectUri()).build();
        }
        authorizationService.complete(state, code);
        log.info("[GITHUB_APP_OAUTH_DIAG] OAuth callback completed");
        return ResponseEntity.status(HttpStatus.FOUND).location(successRedirectUri()).build();
    }

    private URI installationRedirectUri(PartyGithubConnectionService.InstallCompletion completion) {
        String redirectPath = completion.redirectPath() == null
                ? "/parties/" + completion.partyId() : completion.redirectPath();
        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
        return URI.create(baseUrl).resolve(redirectPath.substring(1));
    }

    private URI successRedirectUri() {
        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
        return URI.create(baseUrl).resolve("github-app/authorized");
    }

    private URI installedRedirectUri() {
        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
        return URI.create(baseUrl).resolve("github-app/installed");
    }
}
