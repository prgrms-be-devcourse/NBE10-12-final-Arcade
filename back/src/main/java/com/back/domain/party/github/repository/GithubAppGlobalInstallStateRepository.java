package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.GithubAppGlobalInstallState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubAppGlobalInstallStateRepository extends JpaRepository<GithubAppGlobalInstallState, Long> {
    Optional<GithubAppGlobalInstallState> findByState(String state);
}
