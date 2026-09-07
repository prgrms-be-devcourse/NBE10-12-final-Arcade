package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.GithubAppUserAuthorizationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubAppUserAuthorizationStateRepository extends JpaRepository<GithubAppUserAuthorizationState, Long> {
    Optional<GithubAppUserAuthorizationState> findByState(String state);
}
