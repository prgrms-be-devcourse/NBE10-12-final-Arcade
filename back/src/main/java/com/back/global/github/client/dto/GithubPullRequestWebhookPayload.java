package com.back.global.github.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** pull_request webhook의 routing 정보와 PR 원본 데이터를 표현한다. */
public record GithubPullRequestWebhookPayload(
    GithubRepository repository,
    Installation installation,
    @JsonProperty("pull_request")
    GithubPullRequestResponse pullRequest
) {
    public record GithubRepository(long id) {
    }
    public record Installation(long id) {
    }
}
