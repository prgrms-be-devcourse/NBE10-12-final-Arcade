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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/** webhook 누락을 보정하기 위해 활성 GitHub 연결의 PR 목록을 주기적으로 재동기화한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyGithubPrSyncService {
    private static final int SYNC_BATCH_SIZE = 100;

    private final PartyGithubBindingRepository bindingRepository;
    private final PartyGithubConnectionRepository connectionRepository;
    private final GithubAppClient githubAppClient;
    private final PartyPrService partyPrService;

    @Scheduled(fixedDelayString = "${custom.github.pull-request-sync.fixed-delay-millis:3600000}")
    public void syncActiveConnections() {
        Set<Long> syncedPartyIds = new HashSet<>();
        var pageable = PageRequest.of(0, SYNC_BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        var bindings = bindingRepository.findAllByStatus(PartyGithubBindingStatus.ACTIVE, pageable);
        while (true) {
            bindings.forEach(binding -> {
                if (syncedPartyIds.add(binding.getParty().getId())) sync(binding);
            });
            if (!bindings.hasNext()) break;
            bindings = bindingRepository.findAllByStatus(PartyGithubBindingStatus.ACTIVE, bindings.nextPageable());
        }

        var connections = connectionRepository.findAllByStatus(PartyGithubConnectionStatus.ACTIVE, pageable);
        while (true) {
            connections.forEach(connection -> {
                if (syncedPartyIds.add(connection.getParty().getId())) sync(connection);
            });
            if (!connections.hasNext()) break;
            connections = connectionRepository.findAllByStatus(PartyGithubConnectionStatus.ACTIVE, connections.nextPageable());
        }
    }

    private void sync(PartyGithubBinding binding) {
        try {
            String token = githubAppClient.createInstallationToken(
                    binding.getInstallationRepository().getInstallation().getInstallationId());
            var pullRequests = githubAppClient.getAllPullRequests(token, binding.getInstallationRepository().getFullName());
            long partyId = binding.getParty().getId();
            partyPrService.syncExistingPullRequests(partyId, pullRequests, binding.getTrackingStartedAt());
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
            long partyId = connection.getParty().getId();
            partyPrService.syncExistingPullRequests(partyId, pullRequests);
            log.info("Legacy Party GitHub PR sync completed: partyId={}, count={}", connection.getParty().getId(), pullRequests.size());
        } catch (RuntimeException exception) {
            log.warn("Legacy Party GitHub PR sync failed: partyId={}", connection.getParty().getId(), exception);
        }
    }
}
