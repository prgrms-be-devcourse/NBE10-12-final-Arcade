package com.back.domain.party.partyPr.controller;

import com.back.domain.party.partyPr.dto.PartyPrDto;
import com.back.domain.party.partyPr.service.PartyPrService;
import com.back.domain.party.github.service.PartyGithubConnectionService;
import com.back.global.github.service.GithubWebhookService;
import com.back.domain.party.github.dto.PartyGithubConnectionDto;
import com.back.domain.party.github.dto.GithubAppInstallUrlDto;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1PartyPrController {
    private final PartyPrService partyPrService;
    private final PartyGithubConnectionService githubConnectionService;
    private final GithubWebhookService githubWebhookService;
    private final Rq rq;

    @Value("${custom.frontend.base-url:/}")
    private String frontendBaseUrl;

    @GetMapping("/parties/{partyId}/github-connection")
    public RsData<PartyGithubConnectionDto> getGithubConnection(@PathVariable long partyId) {
        return new RsData<>("200-1", "GitHub 연결 상태 조회 성공", githubConnectionService.getStatus(partyId));
    }

    @PostMapping("/parties/{partyId}/github-app/install")
    public RsData<GithubAppInstallUrlDto> installGithubApp(
            @PathVariable long partyId,
            @RequestParam(required = false) String redirectUrl
    ) {
        return new RsData<>("200-1", "GitHub App 설치 URL 생성 성공", githubConnectionService.beginInstall(partyId, rq.getActorFromDb(), redirectUrl));
    }

    @GetMapping("/github-app/setup")
    public ResponseEntity<Void> githubAppSetup(
        @RequestParam String state,
        @RequestParam(name = "installation_id") long installationId
    ) {
        PartyGithubConnectionService.InstallCompletion completion = githubConnectionService.completeInstall(state, installationId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri(completion))
                .build();
    }

    private URI redirectUri(PartyGithubConnectionService.InstallCompletion completion) {
        String redirectPath = completion.redirectPath() == null
                ? "/parties/" + completion.partyId()
                : completion.redirectPath();
        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";

        return URI.create(baseUrl).resolve(redirectPath.substring(1));
    }

    @GetMapping("/parties/{partyId}/pull-requests")
    public RsData<List<PartyPrDto>> getPullRequests(@PathVariable long partyId) {
        return new RsData<>(
                "200-1",
                "PR 목록 조회 성공",
                partyPrService.getByPartyId(partyId)
        );
    }

    @PostMapping("/github/webhook")
    /** GitHub 공개 webhook endpoint: 일반 로그인 대신 HMAC 서명과 delivery ID로 신뢰성과 중복을 검증한다. */
    public ResponseEntity<Void> githubWebhook(
        @RequestHeader(value = "X-GitHub-Event", required = false) String event,
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
        @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
        @RequestBody byte[] body
    ) {
        githubWebhookService.receive(event, signature, deliveryId, body);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
