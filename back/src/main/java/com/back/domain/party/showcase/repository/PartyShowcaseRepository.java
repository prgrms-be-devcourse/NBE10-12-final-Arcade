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

    @Query("""
        select ps from PartyShowcase ps
        join fetch ps.party p
        join fetch p.owner
        where ps.published = true
        order by p.likeCount desc
        """)
    List<PartyShowcase> findPublishedOrderByPartyLikeCountDesc(Pageable pageable);
}
