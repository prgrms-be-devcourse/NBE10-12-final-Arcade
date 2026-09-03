package com.back.domain.search.search.service.party;

public interface PartySearchKeywordPort {

    void keywordParty(long partyId, String text);
    void deleteKeywordParty(long partyId);
}
