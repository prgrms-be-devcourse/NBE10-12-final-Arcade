package com.back.domain.party.showcase.repository;

import com.back.domain.party.party.entity.Party;
import com.back.domain.party.showcase.entity.PartyShowcase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PartyShowcaseRepository extends JpaRepository<PartyShowcase, Long> {
    Optional<PartyShowcase> findByParty(Party party);

    // 파티 좋아요 수 기준 내림차순
    // 호출부에서 PageRequest.of(0, 3)으로 넘기면 상위 3개만 나옴
    // likeCount가 같을 때 DB가 반환 순서를 보장하지 않아 조회할 때마다 순서가 흔들릴 수 있어 최근 게시된 순으로 2차 정렬 기준을 둬서 순서를 안정적으로 고정한다
    @Query("""
        select ps from PartyShowcase ps
        join fetch ps.party p
        join fetch p.owner
        where ps.published = true
        order by p.likeCount desc, ps.publishedAt desc
        """)

    List<PartyShowcase> findPublishedOrderByPartyLikeCountDesc(Pageable pageable);
}
