package com.back.domain.search.search.repository.party;

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
                SELECT p.id FROM party_search_keyword psk
                JOIN party p ON p.id = psk.party_id
                WHERE to_tsvector('simple', psk.keywords) @@ to_tsquery('simple', :tsQuery)
                  AND p.status = :status
                  AND (:partyTag IS NULL OR p.party_tag = :partyTag)
                  AND (:topicType IS NULL OR p.topic_type = :topicType)
                  AND (:positionType IS NULL OR EXISTS (
                      SELECT 1 FROM "position" pos WHERE pos.party_id = p.id AND pos.type = :positionType
                  ))
                ORDER BY p.id DESC
                """,
            countQuery = """
                SELECT count(*) FROM party_search_keyword psk
                JOIN party p ON p.id = psk.party_id
                WHERE to_tsvector('simple', psk.keywords) @@ to_tsquery('simple', :tsQuery)
                  AND p.status = :status
                  AND (:partyTag IS NULL OR p.party_tag = :partyTag)
                  AND (:topicType IS NULL OR p.topic_type = :topicType)
                  AND (:positionType IS NULL OR EXISTS (
                      SELECT 1 FROM "position" pos WHERE pos.party_id = p.id AND pos.type = :positionType
                  ))
                """,
            nativeQuery = true
    )
    Page<Long> searchPartyIdsByKeywords(
            @Param("tsQuery") String tsQuery,
            @Param("status") String status,
            @Param("partyTag") String partyTag,
            @Param("topicType") String topicType,
            @Param("positionType") String positionType,
            Pageable pageable
    );
}
