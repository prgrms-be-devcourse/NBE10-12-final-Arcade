package com.back.domain.search.search.service.party;

import com.back.domain.party.party.event.PartySearchIndexRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartySearchIndexEventListener {

    private final PartySearchKeywordPort partySearchKeywordPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIndexRequested(PartySearchIndexRequestedEvent event) {
        try {
            partySearchKeywordPort.keywordParty(event.partyId());
        } catch (Exception e) {
            log.warn("파티 검색 색인에 실패했습니다. partyId={}", event.partyId(), e);
        }
    }
}
