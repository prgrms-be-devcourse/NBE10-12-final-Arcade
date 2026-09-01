package com.back.domain.goal.goal.event;

import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.party.party.event.PartyAssembledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * 파티 확정(모집 마감) 이벤트를 받아 참여자별 PROJECT 성취를 자동 생성한다(기획서 3.6, 작업표 9번).
 *
 * AFTER_COMMIT으로 받아 파티 마감 트랜잭션이 성공한 뒤에만 성취를 만든다.
 * 성취 생성이 실패해도 파티 마감은 이미 커밋돼 되돌아가지 않으며, 재실행해도 중복 생성되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class PartyAssembledEventListener {

    private final GoalService goalService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePartyAssembled(PartyAssembledEvent event) {
        goalService.createProjectsForAssembledParty(
                event.partyId(),
                event.approvedMemberIds(),
                // 파티 도메인이 확정 시각을 이벤트에 실어주면 그 값을 쓴다
                // (docs/성취-자동생성_파티도메인_수정요청.md ②)
                LocalDate.now()
        );
    }
}
