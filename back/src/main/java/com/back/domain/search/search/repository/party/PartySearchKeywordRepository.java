package com.back.domain.search.search.repository.party;

import com.back.domain.party.party.entity.Party;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartySearchKeywordRepository extends JpaRepository<PartySearchKeyword, Long>, JpaSpecificationExecutor<PartySearchKeyword> {
    Optional<PartySearchKeyword> findByParty_Id(long partyId);

    @Query(
            value = """
                SELECT p.* FROM party_search_keyword psk
                JOIN party p ON p.id = psk.party_id
                WHERE to_tsvector('simple', psk.keywords) @@ to_tsquery('simple', :tsQuery)
                  AND p.status = :status
                """,
            countQuery = """
                SELECT count(*) FROM party_search_keyword psk
                JOIN party p ON p.id = psk.party_id
                WHERE to_tsvector('simple', psk.keywords) @@ to_tsquery('simple', :tsQuery)
                  AND p.status = :status
                """,
            nativeQuery = true
    )
    Page<Party> searchByKeywords(@Param("tsQuery") String tsQuery, @Param("status") String status, Pageable pageable);
}
