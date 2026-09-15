package com.back.domain.party.github.event;

import com.back.domain.party.github.service.PartyGithubBindingArchiveService;
import com.back.domain.party.party.event.PartyCompletedEvent;
import com.back.domain.party.partyPr.service.PartyPrSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Party 완료 후 Party 단위 GitHub 연결만 archive한다. GitHub App installation과 PR 이력은 삭제하지 않는다. */
@Component
@RequiredArgsConstructor
public class PartyGithubBindingCompletedEventListener {
    private final PartyGithubBindingArchiveService archiveService;
    private final PartyPrSseService partyPrSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PartyCompletedEvent event) {
        archiveService.archivePartyConnection(event.partyId());
        partyPrSseService.completeParty(event.partyId());
    }
}
