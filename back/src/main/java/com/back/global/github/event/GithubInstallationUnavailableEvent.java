package com.back.global.github.event;

/** GitHub App 설치가 삭제되거나 suspend된 사실을 알린다. */
public record GithubInstallationUnavailableEvent(long installationId) {
}
