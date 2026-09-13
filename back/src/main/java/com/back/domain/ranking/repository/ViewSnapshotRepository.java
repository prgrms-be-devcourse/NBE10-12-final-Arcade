package com.back.domain.ranking.repository;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.ranking.entity.ViewSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ViewSnapshotRepository extends JpaRepository<ViewSnapshot, Long> {

    @Query("""
        select s from ViewSnapshot s
        where s.targetType = :targetType and s.targetId in :targetIds and s.snapshotDate >= :from
        """)
    List<ViewSnapshot> findAllByTargetTypeAndTargetIdInAndSnapshotDateGreaterThanEqual(
            @Param("targetType") TargetType targetType,
            @Param("targetIds") Collection<Long> targetIds,
            @Param("from") LocalDate from
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ViewSnapshot s where s.targetType = :targetType and s.snapshotDate = :date")
    void deleteByTargetTypeAndSnapshotDate(@Param("targetType") TargetType targetType, @Param("date") LocalDate date);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ViewSnapshot s where s.snapshotDate < :date")
    void deleteBySnapshotDateBefore(@Param("date") LocalDate date);
}
