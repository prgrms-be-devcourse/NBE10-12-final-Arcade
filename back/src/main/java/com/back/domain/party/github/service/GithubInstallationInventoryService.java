package com.back.domain.party.github.service;

import com.back.domain.party.github.entity.GithubAppInstallation;
import com.back.domain.party.github.entity.GithubInstallationRepository;
import com.back.domain.party.github.repository.GithubAppInstallationRepository;
import com.back.domain.party.github.repository.GithubInstallationRepositoryRepository;
import com.back.global.github.client.GithubAppClient;
import com.back.global.github.event.GithubInstallationRepositoryRemovedEvent;
import com.back.global.github.event.GithubInstallationSyncRequestedEvent;
import com.back.global.github.event.GithubInstallationUnavailableEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.back.global.github.client.dtos.GithubInstallationSnapshot;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** GitHub App installation 및 선택 레포 목록의 서버 inventory를 GitHub 원본과 일치시킨다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GithubInstallationInventoryService {
    private final GithubAppInstallationRepository installationRepository;
    private final GithubInstallationRepositoryRepository repositoryRepository;
    private final GithubAppClient githubAppClient;
    private final PlatformTransactionManager transactionManager;

    @EventListener
    @Transactional
    public void sync(GithubInstallationSyncRequestedEvent event) {
        applySnapshot(event.snapshot());
    }

    /** 호출자는 외부 조회 전에 DB 트랜잭션을 끝내야 한다. */
    @Transactional(propagation = Propagation.NEVER)
    public void syncInstallation(long installationId) {
        GithubInstallationSnapshot snapshot = githubAppClient.getInstallationSnapshot(installationId);
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> applySnapshot(snapshot));
    }

    @Transactional
    public void applySnapshot(GithubInstallationSnapshot snapshot) {
        GithubAppClient.Installation remote = snapshot.installation();
        long installationId = remote.id();
        GithubAppInstallation installation = installationRepository.findByInstallationId(installationId)
                .map(existing -> {
                    existing.refreshAccount(remote.accountGithubId(), remote.accountLogin(), remote.accountType());
                    return existing;
                })
                .orElseGet(() -> installationRepository.save(new GithubAppInstallation(
                        remote.id(), remote.accountGithubId(), remote.accountLogin(), remote.accountType(), null)));

        Map<Long, GithubInstallationRepository> existingById = repositoryRepository
                .findAllByInstallationInstallationId(installationId).stream()
                .collect(Collectors.toMap(GithubInstallationRepository::getRepositoryId, Function.identity()));
        for (GithubAppClient.Repository remoteRepository : snapshot.repositories()) {
            GithubInstallationRepository existing = existingById.remove(remoteRepository.id());
            if (existing != null) existing.refresh(remoteRepository.fullName());
            else repositoryRepository.save(new GithubInstallationRepository(
                    installation, remoteRepository.id(), remoteRepository.fullName()));
        }
        existingById.values().forEach(GithubInstallationRepository::remove);
    }

    @EventListener
    @Transactional
    public void markUnavailable(GithubInstallationUnavailableEvent event) {
        installationRepository.findByInstallationId(event.installationId()).ifPresent(installation -> {
            if (event.deleted()) installation.delete();
            else installation.suspend();
            repositoryRepository.findAllByInstallationInstallationId(event.installationId())
                    .forEach(GithubInstallationRepository::remove);
        });
    }

    @EventListener
    @Transactional
    public void markRepositoryRemoved(GithubInstallationRepositoryRemovedEvent event) {
        repositoryRepository.findByInstallationInstallationIdAndRepositoryId(event.installationId(), event.repositoryId())
                .ifPresent(GithubInstallationRepository::remove);
    }
}
