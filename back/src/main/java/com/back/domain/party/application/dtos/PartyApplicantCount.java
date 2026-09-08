package com.back.domain.party.application.dtos;

/** 파티별 지원자 수. 목록 카드마다 세면 카드 수만큼 쿼리가 나가서 한 번에 집계할 때 쓴다. */
public record PartyApplicantCount(long partyId, long count) {
}
