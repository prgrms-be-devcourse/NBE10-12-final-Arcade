package com.back.domain.party.party.dtos;

import com.back.domain.contest.contest.dtos.ContestSummaryDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.position.dtos.PositionDto;
import com.back.domain.party.position.entity.PartyStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record PartyDto(
    long id,
    long ownerId,
    String ownerName,
    String partyName,
    String title,
    String description,
    ContestSummaryDto targetContest,
    String contestTitle,
    String contestLinkUrl,
    TopicType topicType,
    PartyStatus status,
    PartyTag partyTag,
    String githubRepoUrl,
    int checklistRequiredApprovals,
    LocalDateTime deadline,
    long dDay,
    int likeCount,
    int viewCount,
    List<PositionDto> positions
) {
    public PartyDto(Party party) {
        this(
            party.getId(),
            party.getOwner().getId(),
            party.getOwner().getName(),
            party.getPartyName(),
            party.getTitle(),
            party.getDescription(),
            party.getTargetContest() == null ? null : new ContestSummaryDto(party.getTargetContest()),
            party.getContestTitle(),
            party.getContestLinkUrl(),
            party.getTopicType(),
            party.getStatus(),
            party.getPartyTag(),
            party.getGithubRepoUrl(),
            party.getChecklistRequiredApprovals(),
            party.getDeadline(),
            Duration.between(LocalDateTime.now(), party.getDeadline()).toDays(),
            party.getLikeCount(),
            party.getViewCount(),
            party.getPositions().stream().map(PositionDto::new).toList()
        );
    }
}
