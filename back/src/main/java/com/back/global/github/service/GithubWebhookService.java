package com.back.global.github.service;

import com.back.global.github.client.dto.GithubPullRequestWebhookPayload;
import com.back.global.github.entity.GithubWebhookDelivery;
import com.back.global.github.event.GithubInstallationRepositoryRemovedEvent;
import com.back.global.github.event.GithubInstallationUnavailableEvent;
import com.back.global.github.event.GithubPullRequestReceivedEvent;
import com.back.global.github.repository.GithubWebhookDeliveryRepository;
import com.back.global.exception.ServiceException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

/** GitHub App webhook의 공통 검증·중복 방지와 이벤트별 도메인 라우팅을 담당한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GithubWebhookService {
    private final GithubWebhookVerifier webhookVerifier;
    private final GithubWebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void receive(String event, String signature, String deliveryId, byte[] body) {
        webhookVerifier.verify(signature, body);
        if (event == null || event.isBlank()) throw new ServiceException("400-2", "GitHub 웹훅 이벤트가 없습니다.");
        if (deliveryId == null || deliveryId.isBlank()) throw new ServiceException("400-2", "GitHub 웹훅 delivery ID가 없습니다.");
        if (deliveryRepository.existsByDeliveryId(deliveryId)) return;
        deliveryRepository.save(new GithubWebhookDelivery(deliveryId));

        switch (event) {
            case "pull_request" -> publishPullRequest(readPullRequestPayload(body));
            case "installation" -> handleInstallation(readPayload(body));
            case "installation_repositories" -> handleInstallationRepositories(readPayload(body));
            default -> { /* 서명·delivery 검증 후 지원하지 않는 이벤트는 무시한다. */ }
        }
    }

    private void handleInstallation(JsonNode payload) {
        long installationId = requiredInstallationId(payload);
        String action = payload.path("action").asText();
        if ("deleted".equals(action) || "suspend".equals(action)) {
            eventPublisher.publishEvent(new GithubInstallationUnavailableEvent(installationId));
        }
    }

    private void handleInstallationRepositories(JsonNode payload) {
        long installationId = requiredInstallationId(payload);
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
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ServiceException("400-2", "GitHub 웹훅 본문이 올바르지 않습니다.");
        }
    }

    private GithubPullRequestWebhookPayload readPullRequestPayload(byte[] body) {
        try {
            return objectMapper.readValue(body, GithubPullRequestWebhookPayload.class);
        } catch (Exception e) {
            throw new ServiceException("400-2", "GitHub 웹훅 본문이 올바르지 않습니다.");
        }
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
