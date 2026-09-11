package com.back.domain.party.showcase.ranking.repository;

import com.back.domain.party.showcase.ranking.entity.ShowcaseViewSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ShowcaseViewSnapshotRepository extends JpaRepository<ShowcaseViewSnapshot, Long> {

    @Query("""
        select s from ShowcaseViewSnapshot s
        where s.showcaseId in :showcaseIds and s.snapshotDate >= :from
        """)
    List<ShowcaseViewSnapshot> findAllByShowcaseIdInAndSnapshotDateGreaterThanEqual(
            @Param("showcaseIds") Collection<Long> showcaseIds,
            @Param("from") LocalDate from
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ShowcaseViewSnapshot s where s.snapshotDate = :date")
    void deleteBySnapshotDate(@Param("date") LocalDate date);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ShowcaseViewSnapshot s where s.snapshotDate < :date")
    void deleteBySnapshotDateBefore(@Param("date") LocalDate date);
}
