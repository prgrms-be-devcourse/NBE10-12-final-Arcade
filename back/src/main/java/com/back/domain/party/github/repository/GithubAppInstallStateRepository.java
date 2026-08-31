package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.GithubAppInstallState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubAppInstallStateRepository extends JpaRepository<GithubAppInstallState, Long> {
    Optional<GithubAppInstallState> findByState(String state);
    Optional<GithubAppInstallState> findByPartyId(Long partyId);
}
