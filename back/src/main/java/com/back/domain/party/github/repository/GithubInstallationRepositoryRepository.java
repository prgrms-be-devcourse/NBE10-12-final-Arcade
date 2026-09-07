package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.GithubInstallationRepository;
import com.back.domain.party.github.entity.GithubInstallationRepositoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface GithubInstallationRepositoryRepository extends JpaRepository<GithubInstallationRepository, Long> {
    Optional<GithubInstallationRepository> findByInstallationInstallationIdAndRepositoryId(Long installationId, Long repositoryId);
    @Query("select r from GithubInstallationRepository r join fetch r.installation where r.id = :id")
    Optional<GithubInstallationRepository> findWithInstallationById(@Param("id") Long id);

    @Query("""
            select r from GithubInstallationRepository r join fetch r.installation i
            where i.installationId = :installationId and r.repositoryId in :repositoryIds and r.status = :status
            """)
    List<GithubInstallationRepository> findAccessibleRepositories(
            @Param("installationId") long installationId, @Param("repositoryIds") List<Long> repositoryIds,
            @Param("status") GithubInstallationRepositoryStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GithubInstallationRepository> findById(Long id);
    List<GithubInstallationRepository> findAllByInstallationInstallationId(Long installationId);
    List<GithubInstallationRepository> findAllByInstallationInstallationIdAndStatus(Long installationId, GithubInstallationRepositoryStatus status);
}
