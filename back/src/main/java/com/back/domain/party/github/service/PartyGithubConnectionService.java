package com.back.domain.party.github.service;

import com.back.domain.member.member.entity.Member;
import com.back.global.github.client.GithubAppClient;
import com.back.domain.party.github.dtos.GithubAppInstallUrlDto;
import com.back.domain.party.github.dtos.PartyGithubConnectionDto;
import com.back.domain.party.github.entity.GithubAppInstallState;
import com.back.domain.party.github.entity.GithubAppGlobalInstallState;
import com.back.domain.party.github.entity.PartyGithubConnection;
import com.back.domain.party.github.entity.PartyGithubConnectionStatus;
import com.back.global.github.event.GithubInstallationRepositoryRemovedEvent;
import com.back.global.github.event.GithubInstallationUnavailableEvent;
import com.back.domain.party.github.repository.GithubAppInstallStateRepository;
import com.back.domain.party.github.repository.GithubAppGlobalInstallStateRepository;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import com.back.domain.party.github.repository.PartyGithubBindingRepository;
import com.back.domain.party.github.entity.PartyGithubBindingStatus;
import com.back.domain.party.github.entity.PartyGithubBinding;
import com.back.domain.party.github.entity.GithubAppInstallationStatus;
import com.back.domain.party.github.entity.GithubInstallationRepositoryStatus;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.partyPr.service.PartyPrService;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.back.global.github.client.dtos.GithubInstallationSnapshot;
import com.back.global.github.client.dtos.GithubPullRequestResponse;
import java.util.List;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClientResponseException;

import java.security.SecureRandom;
import java.net.URI;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyGithubConnectionService {

    private final PlatformTransactionManager transactionManager;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyGithubConnectionRepository connectionRepository;
    private final PartyGithubBindingRepository bindingRepository;
    private final GithubAppInstallStateRepository installStateRepository;
    private final GithubAppGlobalInstallStateRepository globalInstallStateRepository;
    private final GithubAppClient githubAppClient;
    private final PartyPrService partyPrService;
    private final PartyGithubConnectionFailureService connectionFailureService;
    private final GithubInstallationInventoryService installationInventoryService;

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

        String expectedRepository = repositoryFullName(party.getGithubRepoUrl());
        if (expectedRepository.isBlank()) {
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
                                    new PartyGithubConnection(party, expectedRepository)));

        connection.awaitInstallation();

        return new GithubAppInstallUrlDto(
                "https://github.com/apps/"
                        + appSlug
                        + "/installations/new?state="
                        + state,
                state);
    }

    /** Party와 무관하게 App 설치 범위를 추가하는 신규 흐름이다. Party binding은 별도 API에서만 만든다. */
    @Transactional
    public GithubAppInstallUrlDto beginGlobalInstall(Member actor) {
        if (actor == null) throw new ServiceException("401-1", "로그인 후 이용해주세요.");
        if (appSlug.isBlank()) throw new IllegalStateException("GitHub App slug가 설정되지 않았습니다.");
        String state = randomState();
        globalInstallStateRepository.save(new GithubAppGlobalInstallState(actor, state));
        return new GithubAppInstallUrlDto("https://github.com/apps/" + appSlug + "/installations/new?state=" + state, state);
    }

    /** 전역 설치 완료 callback은 inventory만 동기화하고 Party binding은 만들지 않는다. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void completeGlobalInstall(String state, long installationId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> globalState(state));
        GithubInstallationSnapshot snapshot = githubAppClient.getInstallationSnapshot(installationId);
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            GithubAppGlobalInstallState installState = globalState(state);
            installationInventoryService.applySnapshot(snapshot);
            installState.consume();
        });
    }

    private GithubAppGlobalInstallState globalState(String state) {
        return globalInstallStateRepository.findByState(state)
                .filter(GithubAppGlobalInstallState::isUsable)
                .orElseThrow(() -> new ServiceException("400-22", "GITHUB_APP_GLOBAL_INSTALL_STATE_INVALID"));
    }

    public boolean isGlobalInstallState(String state) {
        return state != null && globalInstallStateRepository.findByState(state).isPresent();
    }

    /** binding 시도 뒤 선택 레포가 설치 inventory에 없을 때만 기록한다. URL 저장만으로는 호출하지 않는다. */
    @Transactional
    public void markInstallationRequired(long partyId) {
        Party party = partyRepository.findById(partyId).orElseThrow(() -> new ServiceException("404-1", "파티를 찾을 수 없습니다."));
        String repository = repositoryFullName(party.getGithubRepoUrl());
        if (repository.isBlank()) return;
        PartyGithubConnection connection = connectionRepository.findByPartyId(partyId)
                .orElseGet(() -> connectionRepository.save(new PartyGithubConnection(party, repository)));
        connection.markInstallationRequired("GITHUB_APP_INSTALLATION_REPOSITORY_NOT_SELECTED",
                "선택한 GitHub 저장소가 GitHub App 설치 범위에 없습니다.");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InstallCompletion completeInstall(String state, long installationId) {
        InstallPreparation preparation = new TransactionTemplate(transactionManager).execute(ignored -> {
            GithubAppInstallState installState = installStateRepository.findByState(state).filter(GithubAppInstallState::isUsable)
                    .orElseThrow(() -> new ServiceException("400-22", "GITHUB_APP_INSTALL_STATE_INVALID"));
            return new InstallPreparation(installState.getParty().getId(), repositoryFullName(installState.getParty().getGithubRepoUrl()));
        });
        GithubInstallationSnapshot snapshot;
        GithubAppClient.Repository repository;
        List<GithubPullRequestResponse> pullRequests;
        try {
            snapshot = githubAppClient.getInstallationSnapshot(installationId);
            String token = githubAppClient.createInstallationToken(installationId);
            repository = githubAppClient.findRepository(token, preparation.repository());
            pullRequests = githubAppClient.getAllPullRequests(token, repository.fullName());
        } catch (RuntimeException e) {
            // 외부 조회 단계의 실패도 기존 설치 실패 상태로 기록하되 원래 예외를 유지한다.
            try {
                if (e instanceof RestClientResponseException response
                        && (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 404)) {
                    connectionFailureService.markInstallationRequired(preparation.partyId(),
                            "GITHUB_APP_INSTALLATION_UNAVAILABLE", "GitHub App 설치를 다시 확인해주세요.");
                } else connectionFailureService.markError(preparation.partyId(), "GITHUB_APP_ERROR", e.getMessage());
            } catch (RuntimeException ignored) { }
            throw e;
        }
        return new TransactionTemplate(transactionManager).execute(ignored ->
                completeInstallWithSnapshot(state, installationId, preparation.repository(), snapshot, repository, pullRequests));
    }

    private record InstallPreparation(long partyId, String repository) {}

    private InstallCompletion completeInstallWithSnapshot(String state, long installationId, String requestedRepository,
            GithubInstallationSnapshot snapshot, GithubAppClient.Repository repository, List<GithubPullRequestResponse> pullRequests) {
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
        if (!expectedRepository.equals(requestedRepository)) {
            throw new ServiceException("409-20", "GITHUB_INSTALLATION_REPOSITORY_CHANGED");
        }
        PartyGithubConnection connection =
                connectionRepository
                        .findByPartyId(party.getId())
                        .orElseGet(
                                () -> connectionRepository.save(
                                        new PartyGithubConnection(party, expectedRepository)));
        connection.startSync();

        try {
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

            // Party가 지정한 한 레포뿐 아니라 installation 전체 선택 범위를 inventory에 보관한다.
            installationInventoryService.applySnapshot(snapshot);

            partyPrService.syncExistingPullRequests(party, pullRequests);

            connection.activate();
            installState.consume();
            return new InstallCompletion(party.getId(), installState.getRedirectPath());

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 404) {
                markInstallationRequiredAfterCompletion(party.getId(), "GITHUB_APP_INSTALLATION_UNAVAILABLE", "GitHub App 설치를 다시 확인해주세요.");
            } else {
                markErrorAfterCompletion(party.getId(), "GITHUB_APP_ERROR", e.getMessage());
            }
            throw e;
        } catch (RuntimeException e) {
            markErrorAfterCompletion(party.getId(), "GITHUB_APP_ERROR", e.getMessage());
            throw e;
        }
    }

    /**
     * 현재 설치 콜백 트랜잭션이 연결 행을 잠근 상태에서 REQUIRES_NEW를 시작하면 H2가
     * CannotAcquireLockException을 낸다. 원래 트랜잭션이 끝난 뒤에만 실패 상태를 기록한다.
     */
    private void markErrorAfterCompletion(long partyId, String code, String message) {
        runAfterCompletion(() -> connectionFailureService.markError(partyId, code, message));
    }

    private void markInstallationRequiredAfterCompletion(long partyId, String code, String message) {
        runAfterCompletion(() -> connectionFailureService.markInstallationRequired(partyId, code, message));
    }

    private void runAfterCompletion(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK) return;
                try {
                    action.run();
                } catch (RuntimeException ignored) {
                    // 실패 상태 기록이 원래 GitHub 설치 오류를 가리지 않도록 보조 처리로 취급한다.
                }
            }
        });
    }

    public PartyGithubConnectionDto getStatus(long partyId, Member actor) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "파티를 찾을 수 없습니다."));
        if (actor == null || (!party.isOwnedBy(actor)
                && !partyMemberRepository.existsByPartyAndMemberAndStatus(party, actor, PartyMemberStatus.APPROVED))) {
            throw new ServiceException("403-1", "파티장 또는 확정 파티원만 GitHub 연결 상태를 조회할 수 있습니다.");
        }
        if (party.getStatus() == PartyStatus.COMPLETED) return new PartyGithubConnectionDto("ARCHIVED", null, null, null);
        return bindingRepository.findByPartyIdAndStatus(partyId, PartyGithubBindingStatus.ACTIVE)
                .or(() -> bindingRepository.findByPartyIdAndStatus(partyId, PartyGithubBindingStatus.SYNCING))
                .or(() -> bindingRepository.findFirstByPartyIdOrderByIdDesc(partyId))
                .map(this::bindingStatus)
                .or(() -> connectionRepository.findByPartyId(partyId).map(this::legacyStatus))
                .orElse(new PartyGithubConnectionDto("NOT_CONNECTED", null, null, null));
    }

    private PartyGithubConnectionDto bindingStatus(PartyGithubBinding binding) {
        var repository = binding.getInstallationRepository();
        if (binding.getStatus() == PartyGithubBindingStatus.ACTIVE
                || binding.getStatus() == PartyGithubBindingStatus.SYNCING) {
            if (repository.getInstallation().getStatus() != GithubAppInstallationStatus.ACTIVE) {
                return new PartyGithubConnectionDto("INSTALLATION_REQUIRED", repository.getFullName(),
                        "GITHUB_APP_INSTALLATION_UNAVAILABLE", "GitHub App 설치가 제거되었거나 비활성화되었습니다.");
            }
            if (repository.getStatus() != GithubInstallationRepositoryStatus.AVAILABLE) {
                return new PartyGithubConnectionDto("INSTALLATION_REQUIRED", repository.getFullName(),
                        "GITHUB_APP_REPOSITORY_REMOVED", "GitHub App 설치 대상에서 저장소가 제거되었습니다.");
            }
        }
        return new PartyGithubConnectionDto(binding.getStatus().name(), repository.getFullName(), null, null);
    }

    private PartyGithubConnectionDto legacyStatus(PartyGithubConnection connection) {
        if (connection.getStatus() == PartyGithubConnectionStatus.ACTIVE
                || connection.getStatus() == PartyGithubConnectionStatus.SYNCING
                || connection.getStatus() == PartyGithubConnectionStatus.DISCONNECTED) {
            return new PartyGithubConnectionDto(connection);
        }
        if (connection.getStatus() == PartyGithubConnectionStatus.INSTALLATION_REQUIRED) return new PartyGithubConnectionDto(connection);
        if (connection.getStatus() == PartyGithubConnectionStatus.ERROR) return new PartyGithubConnectionDto(connection);
        if (connection.getStatus() == PartyGithubConnectionStatus.ARCHIVED) return new PartyGithubConnectionDto(connection);
        return new PartyGithubConnectionDto("NOT_CONNECTED", null, null, null);
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
