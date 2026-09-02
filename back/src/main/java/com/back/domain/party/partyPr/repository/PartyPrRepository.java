package com.back.domain.party.partyPr.repository;

import com.back.domain.party.partyPr.entity.PartyPr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyPrRepository extends JpaRepository<PartyPr, Long> {
    Optional<PartyPr> findByPartyIdAndGithubPrId(Long partyId, Long githubPrId);

    List<PartyPr> findAllByPartyIdOrderByGithubUpdatedAtDesc(Long partyId);

    // 여러 파티의 PR을 한 번에 조회 - TOP3처럼 파티가 여러 개일 때 파티마다 쿼리 날리는 걸 방지
    List<PartyPr> findAllByPartyIdInOrderByGithubUpdatedAtDesc(List<Long> partyIds);

}
