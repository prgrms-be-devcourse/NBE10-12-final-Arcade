package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.PartyGithubConnection;
import com.back.domain.party.github.entity.PartyGithubConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyGithubConnectionRepository extends JpaRepository<PartyGithubConnection, Long> {
    Optional<PartyGithubConnection> findByPartyId(Long partyId);
    Optional<PartyGithubConnection> findByRepositoryId(Long repositoryId);
    Optional<PartyGithubConnection> findByInstallationIdAndRepositoryId(Long installationId, Long repositoryId);
    List<PartyGithubConnection> findAllByInstallationId(Long installationId);
    List<PartyGithubConnection> findAllByRepositoryIdAndInstallationIdAndStatus(Long repositoryId, Long installationId, PartyGithubConnectionStatus status);
}
