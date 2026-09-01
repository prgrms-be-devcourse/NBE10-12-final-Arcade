package com.back.global.github.event;

import com.back.global.github.client.dtos.GithubPullRequestResponse;

/** 검증된 GitHub pull_request webhook을 Party PR 도메인에 전달하는 내부 이벤트다. */
public record GithubPullRequestReceivedEvent(long installationId, long repositoryId, GithubPullRequestResponse pullRequest) {
}
