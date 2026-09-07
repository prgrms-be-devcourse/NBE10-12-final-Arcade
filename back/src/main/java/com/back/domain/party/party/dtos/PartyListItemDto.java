package com.back.domain.party.party.dtos;

import com.back.domain.contest.contest.entity.ContestFormat;
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
        /** 연결된 등록 대회의 형식(공모전/해커톤). 대회 파티가 아니거나 미등록 외부 대회면 null */
        ContestFormat contestFormat,
        PartyStatus status,
        PartyTag partyTag,
        LocalDateTime deadline,
        long dDay,
        int likeCount,
        int viewCount,
        /** 지원한 사람 수 전체. 승인 인원(positions[].filledCount)과는 다른 값이다 */
        long applicantCount,
        List<PositionDto> positions
) {
    public PartyListItemDto(Party party, long applicantCount) {
        this(
                party.getId(),
                party.getOwner().getName(),
                party.getPartyName(),
                party.getTitle(),
                party.getTopicType(),
                party.getTargetContest() == null ? null : party.getTargetContest().getFormat(),
                party.getStatus(),
                party.getPartyTag(),
                party.getDeadline(),
                Duration.between(LocalDateTime.now(), party.getDeadline()).toDays(),
                party.getLikeCount(),
                party.getViewCount(),
                applicantCount,
                party.getPositions().stream().map(PositionDto::new).toList()
        );
    }
}
