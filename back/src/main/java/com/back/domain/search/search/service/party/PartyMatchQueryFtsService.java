package com.back.domain.search.search.service.party;

import com.back.domain.party.party.entity.Party;
import com.back.domain.search.search.entity.PartySearchKeyword;
import com.back.domain.search.search.repository.PartySearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Profile("prod")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyMatchQueryFtsService implements PartyMatchQueryPort {

    private final PartySearchKeywordRepository partySearchKeywordRepository;

    @Override
    public Page<Party> findMatchingParties(List<String> keywords, Pageable pageable) {
        String tsQuery = keywords.stream().collect(Collectors.joining(" | "));

        return partySearchKeywordRepository.searchByKeywords(tsQuery, pageable)
                .map(PartySearchKeyword::getParty);
    }
}
