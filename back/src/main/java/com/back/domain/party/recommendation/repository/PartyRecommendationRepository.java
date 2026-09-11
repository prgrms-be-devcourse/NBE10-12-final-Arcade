package com.back.domain.party.recommendation.repository;

import com.back.domain.party.recommendation.entity.PartyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartyRecommendationRepository extends JpaRepository<PartyRecommendation, Long> {

    List<PartyRecommendation> findByMemberIdOrderByRank(long memberId);

    @Modifying
    @Query("delete from PartyRecommendation pr where pr.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") long memberId);
}
