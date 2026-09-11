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

    // 재실행 대비 - 오늘자 스냅샷을 지우고 다시 쌓아서 중복이 안 생기게 한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ShowcaseViewSnapshot s where s.snapshotDate = :date")
    void deleteBySnapshotDate(@Param("date") LocalDate date);

    // 30일 창을 넘어간 스냅샷은 더 안 쓰이니 테이블이 계속 커지지 않게 정리한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ShowcaseViewSnapshot s where s.snapshotDate < :date")
    void deleteBySnapshotDateBefore(@Param("date") LocalDate date);
}
