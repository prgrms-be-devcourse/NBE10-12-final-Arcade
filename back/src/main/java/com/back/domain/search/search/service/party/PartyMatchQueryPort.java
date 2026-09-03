package com.back.domain.search.search.service.party;

import com.back.domain.party.party.entity.Party;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartyMatchQueryPort {
    Page<Party> findMatchingParties(List<String> keywords, Pageable pageable);
}
