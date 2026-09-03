package com.back.global.github.client.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/** GitHub REST API와 pull_request webhook이 제공하는 PR 원본 형식이다. */
public record GithubPullRequestResponse(
    long id,
    int number,
    String title,
    @JsonProperty("html_url")
    String htmlUrl,
    String state,
    GithubUser user,
    // GitHub가 일부 응답(특히 설치 직후 기존 PR 조회)에서 null을 내려보낼 수 있다.
    Boolean draft,
    Boolean merged,
    GithubBranch base,
    GithubBranch head,
    @JsonProperty("created_at")
    String createdAt,
    @JsonProperty("closed_at")
    String closedAt,
    @JsonProperty("merged_at")
    String mergedAt,
    @JsonProperty("updated_at")
    String updatedAt
) {
    public record GithubUser(String login) {
    }

    public record GithubBranch(String ref) {
    }
}
