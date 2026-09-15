package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.GithubAppInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubAppInstallationRepository extends JpaRepository<GithubAppInstallation, Long> {
    Optional<GithubAppInstallation> findByInstallationId(Long installationId);
}
