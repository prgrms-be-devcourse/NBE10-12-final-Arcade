package com.back.domain.party.showcase.dtos;

import com.back.domain.party.party.entity.PartyTag;

import java.time.LocalDateTime;

// 전시관 목록 카드용 경량 DTO. PartyShowcaseDto(상세)와 달리 PR 목록·파티원 목록처럼
// 무거운 걸 다 안 채우고, 카드에 실제로 필요한 필드만 담는다.
public record PartyShowcaseListItemDto(
        long partyId,
        String partyName,
        String title,
        String ownerName,
        PartyTag partyTag,
        int viewCount,
        int likeCount,
        LocalDateTime publishedAt
) {
}
