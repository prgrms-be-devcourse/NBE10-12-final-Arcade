package com.back.domain.party.party.dtos;

import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.position.dtos.PositionDto;
import com.back.domain.party.position.entity.PartyStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record PartyListItemDto(
        long id,
        String ownerName,
        String partyName,
        String title,
        TopicType topicType,
        PartyStatus status,
        PartyTag partyTag,
        LocalDateTime deadline,
        long dDay,
        int likeCount,
        int viewCount,
        List<PositionDto> positions
) {
    public PartyListItemDto(Party party) {
        this(
                party.getId(),
                party.getOwner().getName(),
                party.getPartyName(),
                party.getTitle(),
                party.getTopicType(),
                party.getStatus(),
                party.getPartyTag(),
                party.getDeadline(),
                Duration.between(LocalDateTime.now(), party.getDeadline()).toDays(),
                party.getLikeCount(),
                party.getViewCount(),
                party.getPositions().stream().map(PositionDto::new).toList()
        );
    }
}
