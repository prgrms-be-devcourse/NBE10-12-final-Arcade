package com.back.domain.party.github.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.github.dtos.GithubConnectableRepositoryDto;
import com.back.domain.party.github.dtos.PartyGithubBindingDto;
import com.back.domain.party.github.entity.GithubInstallationRepository;
import com.back.domain.party.github.entity.GithubInstallationRepositoryStatus;
import com.back.domain.party.github.entity.PartyGithubBinding;
import com.back.domain.party.github.entity.PartyGithubBindingAudit;
import com.back.domain.party.github.entity.PartyGithubBindingAuditAction;
import com.back.domain.party.github.entity.PartyGithubBindingStatus;
import com.back.domain.party.github.repository.GithubInstallationRepositoryRepository;
import com.back.domain.party.github.repository.PartyGithubBindingAuditRepository;
import com.back.domain.party.github.repository.PartyGithubBindingRepository;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import com.back.domain.party.github.entity.PartyGithubConnectionStatus;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.service.PartyPrService;
import com.back.global.exception.ServiceException;
import com.back.global.github.client.GithubAppClient;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import com.back.domain.party.github.entity.GithubAppInstallationStatus;
import com.back.domain.party.position.entity.PartyStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.back.global.github.client.dtos.GithubPullRequestResponse;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

/** Party장과 GitHub App 설치 범위 접근 권한을 확인한 뒤 Party 단위 binding을 만든다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyGithubBindingService {
    private final PlatformTransactionManager transactionManager;
    private final EntityManager entityManager;
    private final PartyRepository partyRepository;
    private final GithubInstallationRepositoryRepository installationRepositoryRepository;
    private final PartyGithubBindingRepository bindingRepository;
    /** 이전 PartyGithubConnection 기반 연동 데이터와의 중복도 명시적으로 막는다. */
    private final PartyGithubConnectionRepository legacyConnectionRepository;
    private final PartyGithubBindingAuditRepository auditRepository;
    private final GithubAppUserAuthorizationService userAuthorizationService;
    private final GithubAppUserRepositoryAccessService userRepositoryAccessService;
    private final GithubInstallationInventoryService installationInventoryService;
    private final GithubAppClient githubAppClient;
    private final PartyPrService partyPrService;
    private final PartyGithubConnectionService connectionService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<GithubConnectableRepositoryDto> getConnectableRepositories(long partyId, Member actor) {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> ensureOwner(party(partyId), actor));
        return getConnectableRepositories(actor);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<GithubConnectableRepositoryDto> getConnectableRepositories(Member actor) {
        String token = userAuthorizationService.validAccessToken(actor);
        List<GithubAppUserRepositoryAccessService.AccessibleRepository> accessibleRepositories =
                userRepositoryAccessService.repositories(token);
        // 앱이 서버 가동 전부터 설치돼 callback/webhook을 받지 못한 경우에도,
        // 사용자의 접근 가능한 installation을 발견하는 시점에 inventory를 채운다.
        accessibleRepositories.stream()
                .map(GithubAppUserRepositoryAccessService.AccessibleRepository::installationId)
                .distinct()
                .forEach(installationInventoryService::syncInstallation);
        Map<Long, List<Long>> idsByInstallation = accessibleRepositories.stream().collect(Collectors.groupingBy(
                GithubAppUserRepositoryAccessService.AccessibleRepository::installationId,
                Collectors.mapping(GithubAppUserRepositoryAccessService.AccessibleRepository::repositoryId, Collectors.toList())));
        List<GithubConnectableRepositoryDto> result = new ArrayList<>();
        idsByInstallation.forEach((installationId, repositoryIds) -> {
            List<Long> ids = repositoryIds.stream().distinct().toList();
            // IN 절의 파라미터 수를 제한하고 installation/repository 쌍을 유지한다.
            for (int start = 0; start < ids.size(); start += 500) {
                installationRepositoryRepository.findAccessibleRepositories(installationId,
                        ids.subList(start, Math.min(start + 500, ids.size())), GithubInstallationRepositoryStatus.AVAILABLE)
                        .forEach(repository -> result.add(new GithubConnectableRepositoryDto(repository.getId(),
                                installationId, repository.getRepositoryId(), repository.getFullName(),
                                "https://github.com/" + repository.getFullName())));
            }
        });
        return result;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PartyGithubBindingDto connect(long partyId, long installationRepositoryId, Member actor) {
        TransactionTemplate read = new TransactionTemplate(transactionManager);
        read.setReadOnly(true);
        RepositoryKey key = read.execute(ignored -> {
            ensureOwner(party(partyId), actor);
            var repository = installationRepositoryRepository.findWithInstallationById(installationRepositoryId)
                    .orElseThrow(() -> new ServiceException("403-20", "GITHUB_APP_INSTALLATION_REPOSITORY_NOT_SELECTED"));
            return new RepositoryKey(repository.getInstallation().getInstallationId(), repository.getRepositoryId(), repository.getFullName());
        });
        String token = userAuthorizationService.validAccessToken(actor);
        boolean accessible = userRepositoryAccessService.repositories(token).stream().anyMatch(value ->
                value.installationId() == key.installationId() && value.repositoryId() == key.repositoryId());
        if (!accessible) throw new ServiceException("403-20", "GITHUB_REPOSITORY_ACCESS_REQUIRED");
        String installationToken = githubAppClient.createInstallationToken(key.installationId());
        List<GithubPullRequestResponse> pullRequests = githubAppClient.getAllPullRequests(installationToken, key.fullName());
        // 외부 호출이 모두 끝난 후 잠금 아래에서 현재 상태와 중복 여부를 다시 검증한다.
        return new TransactionTemplate(transactionManager).execute(ignored ->
                connectWithLock(partyId, installationRepositoryId, actor, key, pullRequests));
    }

    private PartyGithubBindingDto connectWithLock(long partyId, long installationRepositoryId, Member actor,
                                                  RepositoryKey key, List<GithubPullRequestResponse> pullRequests) {
        // 서로 다른 저장소를 같은 파티에 연결하는 요청도 중복 검사부터 커밋까지 직렬화한다.
        Party party = partyRepository.findByIdForUpdate(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "파티를 찾을 수 없습니다."));
        // OSIV가 유지한 사전 조회 엔티티도 DB의 최신 상태로 검증한다.
        entityManager.refresh(party);
        ensureOwner(party, actor);
        if (party.getStatus() == PartyStatus.COMPLETED) {
            throw new ServiceException("409-20", "PARTY_ALREADY_COMPLETED");
        }
        // 신규 binding 도입 전 이미 연동된 Party는 DB 제약 오류가 아니라 명확한 도메인 오류를 돌려준다.
        if (legacyConnectionRepository.findByPartyId(partyId)
                .filter(connection -> isActive(connection.getStatus())).isPresent()) {
            throw new ServiceException("409-20", "PARTY_GITHUB_REPOSITORY_ALREADY_CONNECTED");
        }
        // 같은 레포를 두 Party에 동시에 연결하는 경쟁 조건을 inventory 행 잠금으로 직렬화한다.
        GithubInstallationRepository repository = installationRepositoryRepository.findById(installationRepositoryId)
                .orElseGet(() -> {
                    connectionService.markInstallationRequired(partyId);
                    throw new ServiceException("403-20", "GITHUB_APP_INSTALLATION_REPOSITORY_NOT_SELECTED");
                });
        entityManager.refresh(repository);
        entityManager.refresh(repository.getInstallation());
        if (repository.getStatus() != GithubInstallationRepositoryStatus.AVAILABLE) {
            throw new ServiceException("409-20", "GITHUB_INSTALLATION_REPOSITORY_UNAVAILABLE");
        }
        if (repository.getInstallation().getStatus() != GithubAppInstallationStatus.ACTIVE
                || repository.getInstallation().getInstallationId() != key.installationId()
                || repository.getRepositoryId() != key.repositoryId()
                || !repository.getFullName().equals(key.fullName())) {
            throw new ServiceException("409-20", "GITHUB_INSTALLATION_REPOSITORY_CHANGED");
        }
        if (bindingRepository.findByPartyIdAndStatus(partyId, PartyGithubBindingStatus.ACTIVE).isPresent()
                || bindingRepository.findByPartyIdAndStatus(partyId, PartyGithubBindingStatus.SYNCING).isPresent()) {
            throw new ServiceException("409-20", "PARTY_GITHUB_REPOSITORY_ALREADY_CONNECTED");
        }
        if (legacyConnectionRepository.findByRepositoryId(repository.getRepositoryId())
                .filter(connection -> isActive(connection.getStatus())).isPresent()) {
            throw new ServiceException("409-20", "GITHUB_REPOSITORY_ALREADY_CONNECTED_TO_ACTIVE_PARTY");
        }
        if (!bindingRepository.findAllByInstallationRepositoryIdAndStatus(repository.getId(), PartyGithubBindingStatus.ACTIVE).isEmpty()
                || !bindingRepository.findAllByInstallationRepositoryIdAndStatus(repository.getId(), PartyGithubBindingStatus.SYNCING).isEmpty()) {
            throw new ServiceException("409-20", "GITHUB_REPOSITORY_ALREADY_CONNECTED_TO_ACTIVE_PARTY");
        }

        PartyGithubBinding binding = bindingRepository.save(new PartyGithubBinding(party, repository, actor));
        auditRepository.save(new PartyGithubBindingAudit(party, repository, actor,
                PartyGithubBindingAuditAction.CONNECTED, null));
        partyPrService.syncExistingPullRequests(party, pullRequests, binding.getTrackingStartedAt());
        binding.activate();
        return new PartyGithubBindingDto(binding);
    }

    @Transactional
    public void disconnect(long partyId, Member actor) {
        Party party = party(partyId);
        ensureOwner(party, actor);
        var binding = bindingRepository.findByPartyIdAndStatus(partyId, PartyGithubBindingStatus.ACTIVE);
        if (binding.isPresent()) {
            binding.get().disconnect("MANUAL_DISCONNECT");
            auditRepository.save(new PartyGithubBindingAudit(party, binding.get().getInstallationRepository(), actor,
                    PartyGithubBindingAuditAction.DISCONNECTED, "MANUAL_DISCONNECT"));
            return;
        }
        var legacyConnection = legacyConnectionRepository.findByPartyId(partyId)
                .filter(connection -> connection.getStatus() != PartyGithubConnectionStatus.DISCONNECTED
                        && connection.getStatus() != PartyGithubConnectionStatus.ARCHIVED);
        if (legacyConnection.isPresent()) {
            legacyConnection.get().disconnect();
            return;
        }
        throw new ServiceException("404-20", "PARTY_GITHUB_REPOSITORY_CONNECTION_NOT_FOUND");
    }

    private Party party(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "파티를 찾을 수 없습니다."));
    }

    private void ensureOwner(Party party, Member actor) {
        if (actor == null || !party.isOwnedBy(actor)) throw new ServiceException("403-1", "파티장만 GitHub 저장소를 관리할 수 있습니다.");
    }

    private boolean isActive(PartyGithubConnectionStatus status) {
        return status == PartyGithubConnectionStatus.ACTIVE || status == PartyGithubConnectionStatus.SYNCING;
    }

    private record RepositoryKey(long installationId, long repositoryId, String fullName) {
    }
}
