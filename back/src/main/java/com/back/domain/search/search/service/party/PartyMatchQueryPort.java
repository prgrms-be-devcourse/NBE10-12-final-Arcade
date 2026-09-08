package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartyMatchQueryPort {
    Page<Long> findMatchingPartyIds(
            List<String> keywords,
            PartyTag partyTag,
            TopicType topicType,
            PositionType positionType,
            Pageable pageable
    );
}
