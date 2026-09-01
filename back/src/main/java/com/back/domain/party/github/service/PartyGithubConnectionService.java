package com.back.domain.party.github.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.github.client.GithubAppClient;
import com.back.domain.party.github.dto.GithubAppInstallUrlDto;
import com.back.domain.party.github.dto.PartyGithubConnectionDto;
import com.back.domain.party.github.entity.GithubAppInstallState;
import com.back.domain.party.github.entity.PartyGithubConnection;
import com.back.global.github.event.GithubInstallationRepositoryRemovedEvent;
import com.back.global.github.event.GithubInstallationUnavailableEvent;
import com.back.domain.party.github.repository.GithubAppInstallStateRepository;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.service.PartyPrService;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.security.SecureRandom;
import java.net.URI;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyGithubConnectionService {

    private final PartyRepository partyRepository;
    private final PartyGithubConnectionRepository connectionRepository;
    private final GithubAppInstallStateRepository installStateRepository;
    private final GithubAppClient githubAppClient;
    private final PartyPrService partyPrService;
    private final PartyGithubConnectionFailureService connectionFailureService;

    @Value("${custom.github.app.slug:}")
    private String appSlug;

    @Transactional
    public GithubAppInstallUrlDto beginInstall(long partyId, Member actor, String redirectUrl) {
        Party party = partyRepository
                .findById(partyId)
                .orElseThrow(() ->
                        new ServiceException(
                                "404-1",
                                "파티를 찾을 수 없습니다.")
                );

        ensureOwner(party, actor);

        if (repositoryFullName(party.getGithubRepoUrl()).isBlank()) {
            throw new ServiceException(
                    "400-22",
                    "GITHUB_REPOSITORY_URL_INVALID"
            );
        }
        if (appSlug.isBlank()) {
            throw new IllegalStateException(
                    "GitHub App slug가 설정되지 않았습니다.");
        }

        String state = randomState();
        String redirectPath = normalizeRedirectPath(redirectUrl);

        installStateRepository
                .findByPartyId(partyId)
                .ifPresentOrElse(
            existing -> existing.renew(state, redirectPath),
                        () -> installStateRepository
                                .save(new GithubAppInstallState(
                                        party, actor, state, redirectPath))
        );

        PartyGithubConnection connection =
                connectionRepository
                    .findByPartyId(partyId)
                    .orElseGet(() ->
                            connectionRepository.save(
                                    new PartyGithubConnection(party)));

        connection.awaitInstallation();

        return new GithubAppInstallUrlDto(
                "https://github.com/apps/"
                        + appSlug
                        + "/installations/new?state="
                        + state,
                state);
    }

    @Transactional
    public InstallCompletion completeInstall(String state, long installationId) {
        GithubAppInstallState installState =
                installStateRepository
                        .findByState(state)
                        .filter(GithubAppInstallState::isUsable)
            .orElseThrow(() ->
                    new ServiceException(
                        "400-22",
                        "GITHUB_APP_INSTALL_STATE_INVALID")
            );

        Party party = installState.getParty();
        String expectedRepository = repositoryFullName(party.getGithubRepoUrl());
        PartyGithubConnection connection =
                connectionRepository
                        .findByPartyId(party.getId())
                        .orElseGet(
                                () -> connectionRepository.save(
                                        new PartyGithubConnection(party)));
        connection.startSync();

        try {
            String token = githubAppClient
                    .createInstallationToken(installationId);
            GithubAppClient.Repository repository =
                    githubAppClient.findRepository(token, expectedRepository);
            connectionRepository.findByRepositoryId(repository.id())
                    .filter(other ->
                            !other.getParty().getId().equals(party.getId()))
                    .ifPresent(other -> {
                        throw new ServiceException(
                                "409-20",
                                "이미 다른 Party에 연결된 GitHub 저장소입니다.");
                    });

            connection.install(
                    installationId,
                    repository.id(),
                    repository.fullName());

            partyPrService.syncExistingPullRequests(
                    party,
                    githubAppClient
                            .getAllPullRequests(
                                    token,
                                    repository.fullName()
                            )
            );

            connection.activate();
            installState.consume();
            return new InstallCompletion(party.getId(), installState.getRedirectPath());

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 404) {
                connectionFailureService.markInstallationRequired(party.getId(), "GITHUB_APP_INSTALLATION_UNAVAILABLE", "GitHub App 설치를 다시 확인해주세요.");
            } else {
                connectionFailureService.markError(party.getId(), "GITHUB_APP_ERROR", e.getMessage());
            }
            throw e;
        } catch (RuntimeException e) {
            connectionFailureService.markError(party.getId(), "GITHUB_APP_ERROR", e.getMessage());
            throw e;
        }
    }

    public PartyGithubConnectionDto getStatus(long partyId) {
        return connectionRepository.findByPartyId(partyId).map(PartyGithubConnectionDto::new)
            .orElse(new PartyGithubConnectionDto("PENDING", null, null, null));
    }

    @EventListener
    @Transactional
    public void handleInstallationUnavailable(GithubInstallationUnavailableEvent event) {
        connectionRepository.findAllByInstallationId(event.installationId()).forEach(connection ->
            connection.markInstallationRequired("GITHUB_APP_INSTALLATION_UNAVAILABLE", "GitHub App 설치가 제거되었거나 비활성화되었습니다."));
    }

    @EventListener
    @Transactional
    public void handleInstallationRepositoryRemoved(GithubInstallationRepositoryRemovedEvent event) {
        connectionRepository.findByInstallationIdAndRepositoryId(event.installationId(), event.repositoryId()).ifPresent(connection ->
            connection.markInstallationRequired("GITHUB_APP_REPOSITORY_REMOVED", "GitHub App 설치 대상에서 저장소가 제거되었습니다."));
    }

    private void ensureOwner(Party party, Member actor) {
        if (actor == null || !party.isOwnedBy(actor))
            throw new ServiceException("403-1", "파티장만 GitHub 저장소를 연동할 수 있습니다.");
    }

    private String repositoryFullName(String url) {

        if (url == null) return "";

        String path = url.trim()
                .replaceAll("[?#].*$", "")
                .replaceFirst("^https?://(www\\.)?github\\.com/", "")
                .replaceFirst("^git@github\\.com:", "")
                .replaceFirst("\\.git/?$", "")
                .replaceFirst("/$", "");

        return path.matches("[^/]+/[^/]+") ? path.toLowerCase(Locale.ROOT) : "";
    }

    private String randomState() {
        byte[] bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String normalizeRedirectPath(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) return null;

        try {
            URI uri = URI.create(redirectUrl);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || !redirectUrl.startsWith("/") || redirectUrl.startsWith("//")) {
                throw new IllegalArgumentException();
            }
            return redirectUrl;
        } catch (IllegalArgumentException e) {
            throw new ServiceException("400-22", "GITHUB_APP_REDIRECT_URL_INVALID");
        }
    }

    public record InstallCompletion(long partyId, String redirectPath) {}
}
