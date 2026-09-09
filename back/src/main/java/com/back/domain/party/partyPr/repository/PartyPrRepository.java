package com.back.domain.party.partyPr.repository;

import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.application.entity.PartyMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyPrRepository extends JpaRepository<PartyPr, Long> {
    Optional<PartyPr> findByPartyIdAndGithubPrId(Long partyId, Long githubPrId);

    List<PartyPr> findAllByPartyIdAndGithubPrIdIn(Long partyId, List<Long> githubPrIds);

    List<PartyPr> findAllByPartyIdOrderByGithubUpdatedAtDesc(Long partyId);

    List<PartyPr> findAllByPartyIdAndAuthorGithubUserIdOrderByGithubUpdatedAtDesc(
            Long partyId,
            Long authorGithubUserId
    );

    List<PartyPr> findAllByPartyIdAndAuthorGithubUserIdIsNullOrderByGithubUpdatedAtDesc(Long partyId);

    List<PartyPr> findAllByAuthorGithubUserIdOrderByGithubUpdatedAtDesc(Long authorGithubUserId);

    @Query("""
            select pr from PartyPr pr where pr.authorGithubUserId = :githubUserId
            and (pr.party.owner = :actor or exists (select pm.id from PartyMember pm where pm.party = pr.party and pm.member = :actor and pm.status = :approvedStatus))
            order by pr.githubUpdatedAt desc
            """)
    List<PartyPr> findReadableByAuthorGithubUserId(@Param("githubUserId") Long githubUserId,
                                                    @Param("actor") com.back.domain.member.member.entity.Member actor,
                                                    @Param("approvedStatus") PartyMemberStatus approvedStatus);

    // 여러 파티의 PR을 한 번에 조회 - TOP3처럼 파티가 여러 개일 때 파티마다 쿼리 날리는 걸 방지
    List<PartyPr> findAllByPartyIdInOrderByGithubUpdatedAtDesc(List<Long> partyIds);

}
