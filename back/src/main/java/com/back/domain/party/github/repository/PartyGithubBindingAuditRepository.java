package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.PartyGithubBindingAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyGithubBindingAuditRepository extends JpaRepository<PartyGithubBindingAudit, Long> {
}
