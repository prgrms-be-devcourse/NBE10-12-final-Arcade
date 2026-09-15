package com.back.domain.party.recommendation.curation;

import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;

/** 큐레이션 프롬프트에 넣을 후보 파티 요약. */
public record PartyCandidate(
        long partyId,
        String title,
        String description,
        TopicType topicType,
        PartyTag partyTag
) {
}
