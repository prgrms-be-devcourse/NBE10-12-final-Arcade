package com.back.domain.party.github.repository;

import com.back.domain.party.github.entity.PartyGithubBinding;
import com.back.domain.party.github.entity.PartyGithubBindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyGithubBindingRepository extends JpaRepository<PartyGithubBinding, Long> {
    Optional<PartyGithubBinding> findFirstByPartyIdOrderByIdDesc(Long partyId);
    Optional<PartyGithubBinding> findByPartyIdAndStatus(Long partyId, PartyGithubBindingStatus status);
    List<PartyGithubBinding> findAllByStatus(PartyGithubBindingStatus status);
    List<PartyGithubBinding> findAllByInstallationRepositoryIdAndStatus(Long installationRepositoryId, PartyGithubBindingStatus status);
}
