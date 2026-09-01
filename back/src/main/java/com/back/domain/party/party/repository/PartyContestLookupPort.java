package com.back.domain.party.party.repository;

import com.back.domain.party.party.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PartyContestLookupPort {

    //모집중(RECRUITING)인 파티가 먼저, 그다음 진행중(IN_PROGRESS),완료(COMPLETED)가 뒤로
    @Query("""
        select p from Party p
        where p.targetContest.id = :contestId
        order by case p.status
            when com.back.domain.party.position.entity.PartyStatus.RECRUITING then 0
            when com.back.domain.party.position.entity.PartyStatus.IN_PROGRESS then 1
            else 2
        end, p.id desc
        """)
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
