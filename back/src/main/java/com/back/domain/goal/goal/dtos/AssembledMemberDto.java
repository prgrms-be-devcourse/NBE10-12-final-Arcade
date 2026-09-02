package com.back.domain.goal.goal.dtos;

import com.back.domain.member.member.entity.PositionType;

/**
 * 파티 확정 시점에 승인돼 있던 참여자 한 명.
 *
 * 파티가 확정되면 그 사건이 PARTY_ASSEMBLE 로, 그 시점의 참여자 명단이 PARTY_ASSEMBLE_TO_MEMBER 로 남는다.
 * {@code partyAssembleToMemberId} 는 그 명단에서 이 사람에 해당하는 행을 가리킨다.
 *
 * 파티 확정 이벤트가 실어온 값을 그대로 담는다. 성취 도메인이 파티 이벤트 타입을 직접 참조하지 않도록
 * 리스너가 이 모양으로 옮겨 담아 서비스에 넘긴다.
 */
public record AssembledMemberDto(
        long memberId,
        long partyAssembleToMemberId,
        PositionType positionType
) { }
