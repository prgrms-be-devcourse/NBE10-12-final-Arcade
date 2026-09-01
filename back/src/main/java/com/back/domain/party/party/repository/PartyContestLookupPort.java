package com.back.domain.party.party.repository;

import com.back.domain.party.party.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PartyContestLookupPort {

    @Query("select p from Party p where p.targetContest.id = :contestId")
    List<Party> findByTargetContestId(@Param("contestId") long contestId);

    interface TeamCount {
        Long getContestId();
        Long getCount();
    }

    @Query("""
      select p.targetContest.id as contestId, count(p) as count
      from Party p
      where p.targetContest.id in :contestIds
      group by p.targetContest.id
      """)
    List<TeamCount> countGroupedByTargetContestIdIn(@Param("contestIds") Collection<Long> contestIds);

}
