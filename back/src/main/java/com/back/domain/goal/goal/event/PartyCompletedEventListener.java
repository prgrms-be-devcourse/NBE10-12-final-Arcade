package com.back.domain.goal.goal.event;

import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.party.party.event.PartyCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 파티 완료 이벤트를 받아 그 파티의 PROJECT 성취를 ACHIEVED로 전이시킨다(기획서 3.6, 작업표 10번).
 *
 * 확정(PartyAssembledEventListener)과 같은 구조다 - AFTER_COMMIT으로 받아
 * 파티 완료 트랜잭션이 성공한 뒤에만 돌고, 여기서 실패해도 파티 완료는 되돌아가지 않는다.
 * 그래서 재실행이 안전해야 하고, 이미 완료된 성취는 서비스에서 걸러진다.
 *
 * 종료일은 이벤트가 실어온 완료 시점을 쓴다. 성취의 endDate는 날짜 단위라 여기서 날짜만 떼어 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class PartyCompletedEventListener {

    private final GoalService goalService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePartyCompleted(PartyCompletedEvent event) {
        goalService.completeProjectsForCompletedParty(
                event.partyId(),
                event.completedAt().toLocalDate()
        );
    }
}
