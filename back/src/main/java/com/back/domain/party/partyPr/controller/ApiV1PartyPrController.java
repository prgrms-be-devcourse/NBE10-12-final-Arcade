package com.back.domain.party.partyPr.controller;

import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.dtos.PartyPrByMemberDto;
import com.back.domain.party.partyPr.service.PartyPrService;
import com.back.domain.party.github.service.PartyGithubConnectionService;
import com.back.domain.party.github.service.GithubInstallationInventoryService;
import com.back.global.github.service.GithubWebhookService;
import com.back.domain.party.github.dtos.PartyGithubConnectionDto;
import com.back.domain.party.github.dtos.GithubAppInstallUrlDto;
import com.back.domain.party.partyPr.service.PartyPrSseService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.global.exception.ServiceException;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1PartyPrController {
    private final PartyPrService partyPrService;
    private final PartyGithubConnectionService githubConnectionService;
    private final GithubInstallationInventoryService githubInstallationInventoryService;
    private final GithubWebhookService githubWebhookService;
    private final PartyPrSseService partyPrSseService;
    private final Rq rq;

    @Value("${custom.frontend.base-url:/}")
    private String frontendBaseUrl;

    @GetMapping("/parties/{partyId}/github-connection")
    public RsData<PartyGithubConnectionDto> getGithubConnection(
            @PathVariable long partyId) {
        return new RsData<>("200-1", "GitHub 연결 상태 조회 성공", githubConnectionService.getStatus(partyId, rq.getActorFromDb()));
    }

    @PostMapping("/parties/{partyId}/github-app/install")
    @Deprecated(forRemoval = false)
    public RsData<GithubAppInstallUrlDto> installGithubApp(
            @PathVariable long partyId,
            @RequestParam(required = false) String redirectUrl
    ) {
        return new RsData<>("200-1", "GitHub App 설치 URL 생성 성공", githubConnectionService.beginInstall(partyId, rq.getActorFromDb(), redirectUrl));
    }

    @GetMapping("/github-app/setup")
    public ResponseEntity<Void> githubAppSetup(
        @RequestParam(required = false) String state,
        @RequestParam(name = "installation_id") long installationId
    ) {
        // GitHub App 페이지에서 직접 설치한 경우에는 service가 발급한 state가 없다.
        // 설치 자체는 inventory로만 동기화하고, Party 연결은 이후 사용자 인증·레포 선택 흐름에서 수행한다.
        if (state == null || state.isBlank()) {
            githubInstallationInventoryService.syncInstallation(installationId);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(directInstallRedirectUri())
                    .build();
        }
        PartyGithubConnectionService.InstallCompletion completion = githubConnectionService.completeInstall(state, installationId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri(completion))
                .build();
    }

    private URI directInstallRedirectUri() {
        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
        return URI.create(baseUrl).resolve("github-app/installed");
    }

    private URI redirectUri(
            PartyGithubConnectionService.InstallCompletion completion) {
        String redirectPath = completion.redirectPath() == null
                ? "/parties/" + completion.partyId()
                : completion.redirectPath();
        String baseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";

        return URI.create(baseUrl).resolve(redirectPath.substring(1));
    }

    @GetMapping("/parties/{partyId}/pull-requests")
    public RsData<List<PartyPrDto>> getPullRequests(
            @PathVariable long partyId) {
        return new RsData<>(
                "200-1",
                "PR 목록 조회 성공",
                partyPrService.getByPartyId(partyId, rq.getActorFromDb())
        );
    }

    @GetMapping("/parties/{partyId}/pull-requests/grouped-by-member")
    public RsData<List<PartyPrByMemberDto>> getPullRequestsGroupedByMember(@PathVariable long partyId) {
        return new RsData<>(
                "200-1",
                "담당자별 PR 목록 조회 성공",
                partyPrService.getByPartyIdGroupedByMember(partyId, rq.getActorFromDb())
        );
    }

    @GetMapping("/parties/{partyId}/pull-requests/members/{memberId}")
    public RsData<PartyPrByMemberDto> getPullRequestsByMember(
            @PathVariable long partyId,
            @PathVariable long memberId
    ) {
        return new RsData<>(
                "200-1",
                "파티원 PR 목록 조회 성공",
                partyPrService.getByPartyIdAndMemberId(partyId, memberId, rq.getActorFromDb())
        );
    }

    @GetMapping(value = "/parties/{partyId}/pull-requests/stream", produces = "text/event-stream")
    public SseEmitter streamPullRequests(
            @PathVariable long partyId,
            HttpServletResponse response) {
        // snapshot 조회에서 Party 권한을 먼저 검증하고, 같은 snapshot을 첫 SSE 이벤트로 전달한다.
        try {
            List<PartyPrDto> snapshot = partyPrService.getByPartyId(partyId, rq.getActorFromDb());
            return partyPrSseService.subscribe(partyId, snapshot);
        } catch (ServiceException exception) {
            // EventSource의 Accept: text/event-stream에서는 JSON 예외 응답의 content negotiation이 실패할 수 있다.
            // 연결 자체를 HTTP 오류로 끝내면 브라우저는 onerror를 호출하고 stream을 열지 않는다.
            response.setStatus(exception.getRsData().statusCode());
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }
    }

    @GetMapping(value = "/parties/{partyId}/pull-requests/grouped-by-member/stream", produces = "text/event-stream")
    public SseEmitter streamPullRequestsGroupedByMember(
            @PathVariable long partyId,
            HttpServletResponse response) {
        try {
            List<PartyPrByMemberDto> snapshot =
                    partyPrService.getByPartyIdGroupedByMember(partyId, rq.getActorFromDb());
            return partyPrSseService.subscribeGrouped(partyId, snapshot);
        } catch (ServiceException exception) {
            return rejectedStream(response, exception);
        }
    }

    @GetMapping(value = "/parties/{partyId}/pull-requests/members/me/stream", produces = "text/event-stream")
    public SseEmitter streamMyPullRequestsInParty(
            @PathVariable long partyId,
            HttpServletResponse response) {
        try {
            var actor = rq.getActor();
            PartyPrByMemberDto snapshot =
                    partyPrService.getByPartyIdAndMemberId(partyId, actor.getId(), actor);
            return partyPrSseService.subscribeMember(partyId, snapshot.githubUserId(), snapshot);
        } catch (ServiceException exception) {
            return rejectedStream(response, exception);
        }
    }

    @GetMapping(value = "/parties/{partyId}/pull-requests/members/{memberId:\\d+}/stream", produces = "text/event-stream")
    public SseEmitter streamPullRequestsByMember(
            @PathVariable long partyId,
            @PathVariable long memberId,
            HttpServletResponse response) {
        try {
            PartyPrByMemberDto snapshot = partyPrService.getByPartyIdAndMemberId(
                    partyId, memberId, rq.getActorFromDb());
            return partyPrSseService.subscribeMember(partyId, snapshot.githubUserId(), snapshot);
        } catch (ServiceException exception) {
            return rejectedStream(response, exception);
        }
    }

    private SseEmitter rejectedStream(HttpServletResponse response, ServiceException exception) {
        response.setStatus(exception.getRsData().statusCode());
        SseEmitter emitter = new SseEmitter(0L);
        emitter.complete();
        return emitter;
    }

    @GetMapping("/pull-requests/me")
    public RsData<List<PartyPrDto>> getMyPullRequests() {
        return new RsData<>("200-1", "내 GitHub PR 목록 조회 성공",
                partyPrService.getMyPullRequests(rq.getActorFromDb()));
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
