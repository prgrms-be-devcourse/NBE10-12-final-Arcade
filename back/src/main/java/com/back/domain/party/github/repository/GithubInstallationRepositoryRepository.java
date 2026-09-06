package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.GithubInstallationRepository;
import com.back.domain.party.github.entity.GithubInstallationRepositoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface GithubInstallationRepositoryRepository extends JpaRepository<GithubInstallationRepository, Long> {
    Optional<GithubInstallationRepository> findByInstallationInstallationIdAndRepositoryId(Long installationId, Long repositoryId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GithubInstallationRepository> findById(Long id);
    List<GithubInstallationRepository> findAllByInstallationInstallationId(Long installationId);
    List<GithubInstallationRepository> findAllByInstallationInstallationIdAndStatus(Long installationId, GithubInstallationRepositoryStatus status);
}
