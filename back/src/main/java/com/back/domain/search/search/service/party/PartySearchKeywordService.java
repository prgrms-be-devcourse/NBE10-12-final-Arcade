package com.back.domain.search.search.service.party;

import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.PartySearchKeyword;
import com.back.domain.search.search.repository.PartySearchKeywordRepository;
import com.back.domain.search.search.service.keyword.KeywordExtractionPort;
import com.back.domain.search.search.service.keyword.KeywordNormalizationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    @Transactional
    public void keywordParty(long partyId, String text) {
        List<String> keywords = keywordExtractionPort.extract(text);
        List<String> normalized = keywordNormalizationPort.normalize(keywords);
        String joined = String.join(" ", normalized);

        partySearchKeywordRepository.findByParty_Id(partyId).ifPresentOrElse(
                existing -> existing.updateKeywords(joined),
                () -> {
                    Party party = partyRepository.getReferenceById(partyId);
                    partySearchKeywordRepository.save(new PartySearchKeyword(party, joined));
                }
        );
    }

    @Override
    @Transactional
    public void deleteKeywordParty(long partyId) {
        partySearchKeywordRepository.findByParty_Id(partyId)
                .ifPresent(partySearchKeywordRepository::delete);
    }

}
