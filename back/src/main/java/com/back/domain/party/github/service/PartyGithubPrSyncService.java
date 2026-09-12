package com.back.domain.party.github.service;

import com.back.domain.party.github.entity.PartyGithubBinding;
import com.back.domain.party.github.entity.PartyGithubBindingStatus;
import com.back.domain.party.github.entity.PartyGithubConnection;
import com.back.domain.party.github.entity.PartyGithubConnectionStatus;
import com.back.domain.party.github.repository.PartyGithubBindingRepository;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import com.back.domain.party.partyPr.service.PartyPrService;
import com.back.global.github.client.GithubAppClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/** webhook 누락을 보정하기 위해 활성 GitHub 연결의 PR 목록을 주기적으로 재동기화한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartyGithubPrSyncService {
    private final PartyGithubBindingRepository bindingRepository;
    private final PartyGithubConnectionRepository connectionRepository;
    private final GithubAppClient githubAppClient;
    private final PartyPrService partyPrService;

    @Scheduled(fixedDelayString = "${custom.github.pull-request-sync.fixed-delay-millis:3600000}")
    public void syncActiveConnections() {
        Set<Long> syncedPartyIds = new HashSet<>();
        bindingRepository.findAllByStatus(PartyGithubBindingStatus.ACTIVE).forEach(binding -> {
            if (syncedPartyIds.add(binding.getParty().getId())) sync(binding);
        });
        connectionRepository.findAllByStatus(PartyGithubConnectionStatus.ACTIVE).forEach(connection -> {
            if (syncedPartyIds.add(connection.getParty().getId())) sync(connection);
        });
    }

    private void sync(PartyGithubBinding binding) {
        try {
            String token = githubAppClient.createInstallationToken(
                    binding.getInstallationRepository().getInstallation().getInstallationId());
            var pullRequests = githubAppClient.getAllPullRequests(token, binding.getInstallationRepository().getFullName());
            partyPrService.syncExistingPullRequests(binding.getParty(), pullRequests, binding.getTrackingStartedAt());
            log.info("Party GitHub PR sync completed: partyId={}, count={}", binding.getParty().getId(), pullRequests.size());
        } catch (RuntimeException exception) {
            log.warn("Party GitHub PR sync failed: partyId={}", binding.getParty().getId(), exception);
        }
    }

    private void sync(PartyGithubConnection connection) {
        try {
            if (connection.getInstallationId() == null || connection.getRepositoryId() == null) return;
            String token = githubAppClient.createInstallationToken(connection.getInstallationId());
            var pullRequests = githubAppClient.getAllPullRequests(token, connection.getRepositoryFullName());
            partyPrService.syncExistingPullRequests(connection.getParty(), pullRequests);
            log.info("Legacy Party GitHub PR sync completed: partyId={}, count={}", connection.getParty().getId(), pullRequests.size());
        } catch (RuntimeException exception) {
            log.warn("Legacy Party GitHub PR sync failed: partyId={}", connection.getParty().getId(), exception);
        }
    }
}
