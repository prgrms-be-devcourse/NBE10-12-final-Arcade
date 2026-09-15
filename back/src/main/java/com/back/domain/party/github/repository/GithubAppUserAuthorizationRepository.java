package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.GithubAppUserAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubAppUserAuthorizationRepository extends JpaRepository<GithubAppUserAuthorization, Long> {
    Optional<GithubAppUserAuthorization> findByMemberId(Long memberId);
    Optional<GithubAppUserAuthorization> findByGithubUserId(Long githubUserId);
}
