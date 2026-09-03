package com.back.domain.search.search.service.party;

import com.back.domain.party.party.entity.Party;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("!prod")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyMatchQueryLikeService implements PartyMatchQueryPort {

    private final PartySearchKeywordRepository partySearchKeywordRepository;

    @Override
    public Page<Party> findMatchingParties(List<String> keywords, Pageable pageable) {
        Specification<PartySearchKeyword> spec = matchesAnyKeyword(keywords).and(isRecruiting());

        return partySearchKeywordRepository.findAll(spec, pageable)
                .map(PartySearchKeyword::getParty);
    }

    private Specification<PartySearchKeyword> matchesAnyKeyword(List<String> keywords) {
        return (root, query, cb) -> keywords.stream()
                .map(keyword -> cb.like(root.get("keywords"), "%" + keyword + "%"))
                .reduce(cb::or)
                .orElseGet(cb::disjunction);
    }

    private Specification<PartySearchKeyword> isRecruiting() {
        return (root, query, cb) -> cb.equal(root.get("party").get("status"), PartyStatus.RECRUITING);
    }
}
