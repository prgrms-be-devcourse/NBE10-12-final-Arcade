package com.back.domain.goal.goal.event;

import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.party.showcase.event.PartyShowcasePublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전시 게시 이벤트를 받아 그 파티의 PROJECT 성취에 전시글을 연결한다(기획서 3.6, 작업표 34번).
 *
 * 이벤트가 title·description 따로 입력하지 않음 -> FK로 지정(과거 이력을 담고 있을 필요가 없다고 판단)
 */
@Component
@RequiredArgsConstructor
public class PartyShowcasePublishedEventListener {

    private final GoalService goalService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePartyShowcasePublished(PartyShowcasePublishedEvent event) {
        goalService.linkShowcaseToProjects(event.partyId());
    }
}
