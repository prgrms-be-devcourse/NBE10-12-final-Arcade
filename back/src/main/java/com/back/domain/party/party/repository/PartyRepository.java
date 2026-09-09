package com.back.domain.party.party.repository;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.position.entity.PartyStatus;
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
        where p.hidden = false
          and (:keyword is null or p.partyName like concat('%', cast(:keyword as string), '%') or p.title like concat('%', cast(:keyword as string), '%'))
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

    // 관리자 목록 - 숨김 여부와 무관하게 전부 조회 대상. hidden이 null이면 전체, 값이 있으면 그 값으로 필터.
    @Query("""
    select p from Party p
    where (:hidden is null or p.hidden = :hidden)
      and (:keyword is null or p.partyName like concat('%', cast(:keyword as string), '%') or p.title like concat('%', cast(:keyword as string), '%'))
    """)
    Page<Party> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("hidden") Boolean hidden,
            Pageable pageable
    );

    // 빈자리 합산(capacity-filledCount) 기준 정렬 - 집계라 Pageable Sort로 못 하고 쿼리에 직접 ORDER BY
    // 호출 시 Pageable은 반드시 Sort.unsorted()로 넘겨야 함 (안 그러면 Pageable Sort가 뒤에 덧붙어 충돌)
    @Query(
            value = """
            select p from Party p
            left join p.positions pos
            where p.hidden = false
              and (:keyword is null or p.partyName like concat('%', cast(:keyword as string), '%') or p.title like concat('%', cast(:keyword as string), '%'))
              and (:partyTag is null or p.partyTag = :partyTag)
              and (:positionType is null or exists (
                  select 1 from Position pos2 where pos2.party = p and pos2.type = :positionType
              ))
            group by p
            order by sum(pos.capacity - pos.filledCount) desc
            """,
            countQuery = """
            select count(p) from Party p
            where p.hidden = false
              and (:keyword is null or p.partyName like concat('%', cast(:keyword as string), '%') or p.title like concat('%', cast(:keyword as string), '%'))
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

    // 홈 "인기 파티 TOP3" - 모집 중인 파티만, 좋아요 수 내림차순. 완료/진행중인 파티까지 섞이면
    // "지금 못 들어가는 파티"가 인기 위젯에 뜨는 게 이상해서 RECRUITING으로 제한한다.
    // 동점일 때 조회할 때마다 순서가 흔들리지 않도록 id를 보조 정렬키로 둔다.
    @Query("""
        select p from Party p
        join fetch p.owner
        where p.status = :status and p.hidden = false
        order by p.likeCount desc, p.id desc
        """)
    List<Party> findTopByStatusOrderByLikeCountDesc(@Param("status") PartyStatus status, Pageable pageable);
}
