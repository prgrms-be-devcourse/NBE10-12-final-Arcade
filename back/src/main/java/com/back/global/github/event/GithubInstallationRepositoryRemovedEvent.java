package com.back.global.github.event;

/** GitHub App installation의 접근 대상에서 저장소가 제거됐음을 알린다. */
public record GithubInstallationRepositoryRemovedEvent(long installationId, long repositoryId) {
}
