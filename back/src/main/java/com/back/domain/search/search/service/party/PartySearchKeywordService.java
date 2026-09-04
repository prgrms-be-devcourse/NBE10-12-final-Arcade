package com.back.domain.search.search.service.party;

import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import com.back.domain.search.search.service.keyword.KeywordExtractionPort;
import com.back.domain.search.search.service.keyword.KeywordNormalizationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartySearchKeywordService implements PartySearchKeywordPort {

    private final PartySearchKeywordRepository partySearchKeywordRepository;
    private final KeywordExtractionPort keywordExtractionPort;
    private final KeywordNormalizationPort keywordNormalizationPort;
    private final PartyRepository partyRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void keywordParty(long partyId) {
        Party party = partyRepository.findByIdForUpdate(partyId).orElseThrow();

        List<String> keywords = keywordExtractionPort.extract(party.getTitle());
        List<String> normalized = keywordNormalizationPort.normalize(keywords);
        String joined = String.join(" ", normalized);

        partySearchKeywordRepository.findByParty_Id(partyId).ifPresentOrElse(
                existing -> existing.updateKeywords(joined),
                () -> partySearchKeywordRepository.save(new PartySearchKeyword(party, joined))
        );
    }

    @Override
    @Transactional
    public void deleteKeywordParty(long partyId) {
        partySearchKeywordRepository.findByParty_Id(partyId)
                .ifPresent(partySearchKeywordRepository::delete);
    }

}
