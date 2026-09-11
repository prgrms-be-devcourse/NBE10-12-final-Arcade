package com.back.domain.party.showcase.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.showcase.entity.PartyShowcase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PartyShowcaseRepository extends JpaRepository<PartyShowcase, Long> {
    Optional<PartyShowcase> findByParty(Party party);

    // TOP3 배치 대상 전체 - 후보군 자체가 적어 페이징 없이 한 번에 읽는다.
    List<PartyShowcase> findAllByPublishedTrue();

    // 모집 단계 전용이 아니라 PartyShowcase.viewCount(전시 단계 전용)를 쓴다
    // 기획서 3.2 팀 논의 갱신으로 두 카운터가 완전히 분리됐다.
    // 호출부에서 PageRequest.of(0, 3)으로 넘기면 상위 3개만 나옴
    // party/owner를 fetch join해서, toDto 조립 시 owner.getName() 호출로 인한 지연로딩 쿼리를 없앤다
    // viewCount가 같을 때 DB가 반환 순서를 보장하지 않아 조회할 때마다 순서가 흔들릴 수 있어서, 최근 게시된 순으로 2차 정렬 기준을 둬서 순서를 안정적으로 고정한다
    @Query("""
        select ps from PartyShowcase ps
        join fetch ps.party p
        join fetch p.owner
        where ps.published = true
        order by ps.viewCount desc, ps.publishedAt desc
        """)
    List<PartyShowcase> findPublishedOrderByViewCountDesc(Pageable pageable);

    // 마이페이지 요약의 '전시' 건수 - 내가 확정 명단에 든 파티 중 전시가 게시된 것.
    @Query("""
            select count(ps) from PartyShowcase ps
            where ps.published = true
              and ps.party in (
                  select atm.partyAssemble.party from PartyAssembleToMember atm
                  where atm.member = :member
              )
            """)
    long countPublishedByAssembledMember(@Param("member") Member member);

    // 공개 프로필 '참여한 프로젝트' 목록 - 위 건수 쿼리와 **같은 조건**이어야 한다.
    // 기준이 갈리면 같은 화면에서 '자동기록 3건' 인데 카드가 2장 뜨는 일이 생긴다.
    // party 를 fetch join 해서 카드마다 파티를 다시 읽지 않게 한다.
    @Query("""
            select ps from PartyShowcase ps
            join fetch ps.party
            where ps.published = true
              and ps.party in (
                  select atm.partyAssemble.party from PartyAssembleToMember atm
                  where atm.member = :member
              )
            order by ps.publishedAt desc
            """)
    List<PartyShowcase> findPublishedByAssembledMember(@Param("member") Member member);

    // 전시관 목록 - 게시된 것만 분야(partyTag) 필터는 null이면 전체
    // party/owner를 fetch join해서 목록 페이지에서 파티마다 추가 쿼리 나가는 걸 막는다
    @Query("""
        select ps from PartyShowcase ps
        join fetch ps.party p
        join fetch p.owner
        where ps.published = true
        and (:partyTag is null or p.partyTag = :partyTag)
        order by ps.publishedAt desc
        """)
    Page<PartyShowcase> findPublished(@Param("partyTag") PartyTag partyTag, Pageable pageable);

    // 전시글 좋아요/조회수 - Party.likeCount/viewCount와 동일하게 원자적 UPDATE로 Lost Update 방지
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PartyShowcase ps set ps.likeCount = ps.likeCount + 1 where ps.id = :id")
    void increaseLikeCount(@Param("id") long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PartyShowcase ps set ps.likeCount = case when ps.likeCount > 0 then ps.likeCount - 1 else 0 end where ps.id = :id")
    void decreaseLikeCount(@Param("id") long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PartyShowcase ps set ps.viewCount = ps.viewCount + 1 where ps.id = :id")
    void increaseViewCount(@Param("id") long id);

    // 참여 파티 히스토리의 '전시 페이지 보기' 판정 - 파티마다 조회하지 않으려고 id 목록으로 한 번에 묻는다.
    @Query("select ps.party.id from PartyShowcase ps where ps.published = true and ps.party.id in :partyIds")
    List<Long> findPublishedPartyIdsIn(@Param("partyIds") Collection<Long> partyIds);
}
