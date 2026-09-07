package com.back.domain.search.search.service.party;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartyMatchQueryPort {
    Page<Long> findMatchingPartyIds(
            List<String> keywords,
            String partyTag,
            String topicType,
            String positionType,
            Pageable pageable
    );
}
