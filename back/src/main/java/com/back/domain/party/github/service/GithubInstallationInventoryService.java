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

import java.util.HashSet;
import java.util.Set;

/** GitHub App installation 및 선택 레포 목록의 서버 inventory를 GitHub 원본과 일치시킨다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GithubInstallationInventoryService {
    private final GithubAppInstallationRepository installationRepository;
    private final GithubInstallationRepositoryRepository repositoryRepository;
    private final GithubAppClient githubAppClient;

    @EventListener
    @Transactional
    public void sync(GithubInstallationSyncRequestedEvent event) {
        syncInstallation(event.installationId());
    }

    /** 설치 callback에서도 사용한다. GitHub App JWT가 실제 installation을 확인하므로 직접 설치에도 안전하다. */
    @Transactional
    public void syncInstallation(long installationId) {
        GithubAppClient.Installation remote = githubAppClient.getInstallation(installationId);
        GithubAppInstallation installation = installationRepository.findByInstallationId(installationId)
                .map(existing -> {
                    existing.refreshAccount(remote.accountGithubId(), remote.accountLogin(), remote.accountType());
                    return existing;
                })
                .orElseGet(() -> installationRepository.save(new GithubAppInstallation(
                        remote.id(), remote.accountGithubId(), remote.accountLogin(), remote.accountType(), null)));

        String token = githubAppClient.createInstallationToken(installationId);
        Set<Long> selectedIds = new HashSet<>();
        for (GithubAppClient.Repository remoteRepository : githubAppClient.getAllInstallationRepositories(token)) {
            selectedIds.add(remoteRepository.id());
            repositoryRepository.findByInstallationInstallationIdAndRepositoryId(installationId, remoteRepository.id())
                    .ifPresentOrElse(
                            existing -> existing.refresh(remoteRepository.fullName()),
                            () -> repositoryRepository.save(new GithubInstallationRepository(
                                    installation, remoteRepository.id(), remoteRepository.fullName())));
        }

        repositoryRepository.findAllByInstallationInstallationId(installationId).stream()
                .filter(repository -> !selectedIds.contains(repository.getRepositoryId()))
                .forEach(GithubInstallationRepository::remove);
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
