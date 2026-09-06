package com.back.global.github.service;

import com.back.global.github.client.dtos.GithubPullRequestWebhookPayload;
import com.back.global.github.entity.GithubWebhookDelivery;
import com.back.global.github.event.GithubInstallationRepositoryRemovedEvent;
import com.back.global.github.event.GithubInstallationSyncRequestedEvent;
import com.back.global.github.event.GithubInstallationUnavailableEvent;
import com.back.global.github.event.GithubPullRequestReceivedEvent;
import com.back.global.github.repository.GithubWebhookDeliveryRepository;
import com.back.global.exception.ServiceException;
import com.back.standard.util.Util;
import lombok.RequiredArgsConstructor;
import com.back.global.github.client.GithubAppClient;
import com.back.global.github.client.dtos.GithubInstallationSnapshot;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.JsonNode;

/** GitHub App webhook의 공통 검증·중복 방지와 이벤트별 도메인 라우팅을 담당한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GithubWebhookService {
    private final GithubWebhookVerifier webhookVerifier;
    private final GithubAppClient githubAppClient;
    private final PlatformTransactionManager transactionManager;
    private final GithubWebhookDeliveryRepository deliveryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void receive(String event, String signature, String deliveryId, byte[] body) {
        webhookVerifier.verify(signature, body);
        if (event == null || event.isBlank()) throw new ServiceException("400-2", "GitHub 웹훅 이벤트가 없습니다.");
        if (deliveryId == null || deliveryId.isBlank()) throw new ServiceException("400-2", "GitHub 웹훅 delivery ID가 없습니다.");
        if (deliveryRepository.existsByDeliveryId(deliveryId)) return;
        JsonNode payload = ("installation".equals(event) || "installation_repositories".equals(event))
                ? readPayload(body) : null;
        boolean syncRequired = payload != null && ("installation".equals(event)
                ? java.util.Set.of("created", "unsuspend", "new_permissions_accepted").contains(payload.path("action").asText())
                : payload.path("repositories_added").isArray() && !payload.path("repositories_added").isEmpty());
        GithubInstallationSnapshot snapshot = syncRequired
                ? githubAppClient.getInstallationSnapshot(requiredInstallationId(payload)) : null;
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            // 외부 조회 중 동일 delivery가 처리됐을 수 있으므로 쓰기 직전에 다시 검사한다.
            if (deliveryRepository.existsByDeliveryId(deliveryId)) return;
            deliveryRepository.save(new GithubWebhookDelivery(deliveryId));
            switch (event) {
                case "pull_request" -> publishPullRequest(readPullRequestPayload(body));
                case "installation" -> handleInstallation(payload, snapshot);
                case "installation_repositories" -> handleInstallationRepositories(payload, snapshot);
                default -> { /* 서명·delivery 검증 후 지원하지 않는 이벤트는 무시한다. */ }
            }
        });
    }

    private void handleInstallation(JsonNode payload, GithubInstallationSnapshot snapshot) {
        long installationId = requiredInstallationId(payload);
        String action = payload.path("action").asText();
        if ("deleted".equals(action) || "suspend".equals(action)) {
            eventPublisher.publishEvent(new GithubInstallationUnavailableEvent(installationId, "deleted".equals(action)));
            return;
        }
        if ("created".equals(action) || "unsuspend".equals(action) || "new_permissions_accepted".equals(action)) {
            eventPublisher.publishEvent(new GithubInstallationSyncRequestedEvent(snapshot));
        }
    }

    private void handleInstallationRepositories(JsonNode payload, GithubInstallationSnapshot snapshot) {
        long installationId = requiredInstallationId(payload);
        JsonNode added = payload.path("repositories_added");
        if (added.isArray() && !added.isEmpty()) {
            // 추가 레포의 이름 변경·기존 remove 복구까지 한 번에 맞추기 위해 전체 inventory를 동기화한다.
            eventPublisher.publishEvent(new GithubInstallationSyncRequestedEvent(snapshot));
        }
        JsonNode removed = payload.path("repositories_removed");
        if (!removed.isArray()) return;
        for (JsonNode repository : removed) {
            long repositoryId = repository.path("id").asLong();
            if (repositoryId <= 0) continue;
            eventPublisher.publishEvent(new GithubInstallationRepositoryRemovedEvent(installationId, repositoryId));
        }
    }

    private long requiredInstallationId(JsonNode payload) {
        long installationId = payload.path("installation").path("id").asLong();
        if (installationId <= 0) throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다: installation.id");
        return installationId;
    }

    private JsonNode readPayload(byte[] body) {
        JsonNode payload = Util.json.readTree(body);
        if (payload == null) throw new ServiceException("400-2", "GitHub 웹훅 본문이 올바르지 않습니다.");
        return payload;
    }

    private GithubPullRequestWebhookPayload readPullRequestPayload(byte[] body) {
        GithubPullRequestWebhookPayload payload = Util.json.fromBytes(body, GithubPullRequestWebhookPayload.class);
        if (payload == null) throw new ServiceException("400-2", "GitHub 웹훅 본문이 올바르지 않습니다.");
        return payload;
    }

    private void publishPullRequest(GithubPullRequestWebhookPayload payload) {
        long repositoryId = payload.repository() == null ? 0 : payload.repository().id();
        long installationId = payload.installation() == null ? 0 : payload.installation().id();
        if (repositoryId <= 0 || installationId <= 0 || payload.pullRequest() == null) {
            throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다.");
        }
        eventPublisher.publishEvent(new GithubPullRequestReceivedEvent(installationId, repositoryId, payload.pullRequest()));
    }
}
