package com.back.domain.party.partyPr.model;

import java.time.OffsetDateTime;

/**
 * 외부 GitHub 형식을 PartyPr 생성·갱신에 필요한 내부 상태로 변환한 불변 snapshot이다.
 * 프론트 응답 DTO나 GitHub JSON 형식에 종속되지 않는다.
 */
public record GithubPullRequestSnapshot(
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
}
