package com.back.domain.ranking.repository;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.ranking.entity.FeaturedRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeaturedRankingRepository extends JpaRepository<FeaturedRanking, Long> {

    List<FeaturedRanking> findAllByTargetTypeOrderByRankAsc(TargetType targetType);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FeaturedRanking f where f.targetType = :targetType")
    void deleteAllByTargetType(@Param("targetType") TargetType targetType);
}
