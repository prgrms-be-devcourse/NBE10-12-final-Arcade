package com.back.domain.search.search.repository;

import com.back.domain.search.search.entity.PartySearchKeyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartySearchKeywordRepository extends JpaRepository<PartySearchKeyword, Long> {
    Optional<PartySearchKeyword> findByParty_Id(long partyId);

    @Query(
            value = """
                SELECT psk.* FROM party_search_keyword psk
                JOIN party p ON p.id = psk.party_id
                WHERE to_tsvector('simple', psk.keywords) @@ to_tsquery('simple', :tsQuery)
                  AND p.status = 'RECRUITING'
                """,
            countQuery = """
                SELECT count(*) FROM party_search_keyword psk
                JOIN party p ON p.id = psk.party_id
                WHERE to_tsvector('simple', psk.keywords) @@ to_tsquery('simple', :tsQuery)
                  AND p.status = 'RECRUITING'
                """,
            nativeQuery = true
    )
    Page<PartySearchKeyword> searchByKeywords(@Param("tsQuery") String tsQuery, Pageable pageable);
}
