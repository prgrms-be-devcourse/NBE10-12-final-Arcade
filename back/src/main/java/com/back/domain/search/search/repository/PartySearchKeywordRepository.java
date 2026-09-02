package com.back.domain.search.search.repository;

import com.back.domain.search.search.entity.PartySearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartySearchKeywordRepository extends JpaRepository<PartySearchKeyword, Long> {
    Optional<PartySearchKeyword> findByParty_Id(long partyId);
}
