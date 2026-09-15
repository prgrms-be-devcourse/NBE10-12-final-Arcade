package com.back.domain.goal.goal.event;

import com.back.domain.goal.goal.dtos.AssembledMemberDto;
import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.party.party.event.PartyAssembledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 파티 확정(모집 마감) 이벤트를 받아 참여자별 PROJECT 성취를 자동 생성한다(기획서 3.6, 작업표 9번).
 *
 * AFTER_COMMIT으로 받아 파티 마감 트랜잭션이 성공한 뒤에만 성취를 만든다.
 * 성취 생성이 실패해도 파티 마감은 이미 커밋돼 되돌아가지 않으며, 재실행해도 중복 생성되지 않는다.
 *
 * 이벤트가 확정 시점의 명단(PARTY_ASSEMBLE_TO_MEMBER)과 포지션을 그대로 실어오므로 값을 다시 조회하지 않는다.
 * 나중에 파티원 구성이 바뀌어도 이 성취는 '확정 당시' 기록으로 남아야 하기 때문에,
 * 조회해서 맞추는 것보다 사건이 실어보낸 값을 그대로 쓰는 편이 맞다.
 */
@Component
@RequiredArgsConstructor
public class PartyAssembledEventListener {

    private final GoalService goalService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePartyAssembled(PartyAssembledEvent event) {
        goalService.createProjectsForAssembledParty(
                event.partyId(),
                event.approvedMembers().stream()
                        .map(member -> new AssembledMemberDto(
                                member.memberId(),
                                member.partyAssembleToMemberId(),
                                member.positionType()
                        ))
                        .toList(),
                event.assembledAt()
        );
    }
}
