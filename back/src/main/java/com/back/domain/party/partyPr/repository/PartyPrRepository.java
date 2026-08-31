package com.back.domain.party.partyPr.repository;

import com.back.domain.party.partyPr.entity.PartyPr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyPrRepository extends JpaRepository<PartyPr, Long> {
    Optional<PartyPr> findByPartyIdAndGithubPrId(Long partyId, Long githubPrId);

    List<PartyPr> findAllByPartyIdOrderByGithubUpdatedAtDesc(Long partyId);
}
