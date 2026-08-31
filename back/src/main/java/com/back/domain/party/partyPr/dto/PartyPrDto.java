package com.back.domain.party.partyPr.dto;

import com.back.domain.party.partyPr.entity.PartyPr;

import java.time.OffsetDateTime;

public record PartyPrDto(
    long id,
    long githubPrId,
    int number,
    String title,
    String htmlUrl,
    String state,
    String authorLogin,
    boolean draft,
    boolean merged,
    String baseBranch,
    String headBranch,
    OffsetDateTime openedAt,
    OffsetDateTime closedAt,
    OffsetDateTime mergedAt,
    OffsetDateTime githubUpdatedAt
) {
    public PartyPrDto(PartyPr partyPr) {
        this(
            partyPr.getId(), partyPr.getGithubPrId(), partyPr.getNumber(), partyPr.getTitle(),
            partyPr.getHtmlUrl(), partyPr.getState(), partyPr.getAuthorLogin(), partyPr.isDraft(),
            partyPr.isMerged(), partyPr.getBaseBranch(), partyPr.getHeadBranch(), partyPr.getOpenedAt(),
            partyPr.getClosedAt(), partyPr.getMergedAt(), partyPr.getGithubUpdatedAt()
        );
    }
}
