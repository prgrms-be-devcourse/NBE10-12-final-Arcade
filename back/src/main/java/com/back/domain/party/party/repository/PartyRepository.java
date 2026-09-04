package com.back.domain.party.party.repository;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long>,PartyContestLookupPort{
    @Query("""
        select p from Party p
        where (:keyword is null or p.partyName like concat('%', :keyword, '%') or p.title like concat('%', :keyword, '%'))
          and (:partyTag is null or p.partyTag = :partyTag)
          and (:positionType is null or exists (
              select 1 from Position pos where pos.party = p and pos.type = :positionType
          ))
        """)
    Page<Party> search(
            @Param("keyword") String keyword,
            @Param("partyTag") PartyTag partyTag,
            @Param("positionType") PositionType positionType,
            Pageable pageable
    );

    // 빈자리 합산(capacity-filledCount) 기준 정렬 - 집계라 Pageable Sort로 못 하고 쿼리에 직접 ORDER BY
    // 호출 시 Pageable은 반드시 Sort.unsorted()로 넘겨야 함 (안 그러면 Pageable Sort가 뒤에 덧붙어 충돌)
    @Query(
            value = """
            select p from Party p
            left join p.positions pos
            where (:keyword is null or p.partyName like concat('%', :keyword, '%') or p.title like concat('%', :keyword, '%'))
              and (:partyTag is null or p.partyTag = :partyTag)
              and (:positionType is null or exists (
                  select 1 from Position pos2 where pos2.party = p and pos2.type = :positionType
              ))
            group by p
            order by sum(pos.capacity - pos.filledCount) desc
            """,
            countQuery = """
            select count(p) from Party p
            where (:keyword is null or p.partyName like concat('%', :keyword, '%') or p.title like concat('%', :keyword, '%'))
              and (:partyTag is null or p.partyTag = :partyTag)
              and (:positionType is null or exists (
                  select 1 from Position pos2 where pos2.party = p and pos2.type = :positionType
              ))
            """
    )
    Page<Party> searchOrderByVacancy(
            @Param("keyword") String keyword,
            @Param("partyTag") PartyTag partyTag,
            @Param("positionType") PositionType positionType,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Party p set p.likeCount = p.likeCount + 1 where p.id = :id")
    void increaseLikeCount(@Param("id") long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Party p set p.likeCount = case when p.likeCount > 0 then p.likeCount - 1 else 0 end where p.id = :id")
    void decreaseLikeCount(@Param("id") long id);

    @Query("select p from Party p join fetch p.owner where p.id in :ids")
    List<Party> findAllByIdIn(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Party p where p.id = :id")
    Optional<Party> findByIdForUpdate(@Param("id") long id);
}
