package com.back.domain.party.party.repository;

import com.back.domain.party.party.entity.Party;
import com.back.domain.party.position.entity.PartyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PartyContestLookupPort {

    @Query("""
        select p from Party p
        join fetch p.owner
        where p.targetContest.id = :contestId
        order by case
            when p.status = :recruiting then 0
            when p.status = :inProgress then 1
            else 2
        end, p.id desc
        """)
    List<Party> findByTargetContestId(
            @Param("contestId") long contestId,
            @Param("recruiting") PartyStatus recruiting,
            @Param("inProgress") PartyStatus inProgress
    );

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
