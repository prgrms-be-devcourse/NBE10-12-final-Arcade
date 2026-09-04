package com.back.domain.search.search.service.party;

import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
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
    public Page<Long> findMatchingPartyIds(List<String> keywords, Pageable pageable) {
        String tsQuery = keywords.stream()
                .map(keyword -> "'" + keyword.replace("\\", "\\\\").replace("'", "''") + "'")
                .collect(Collectors.joining(" | "));

        return partySearchKeywordRepository.searchPartyIdsByKeywords(tsQuery, PartyStatus.RECRUITING.name(), pageable);
    }
}
