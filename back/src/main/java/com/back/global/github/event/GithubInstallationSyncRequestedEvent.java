package com.back.global.github.event;

/** installation 생성·재활성화·레포 추가 뒤 서버 inventory를 GitHub 원본으로 다시 동기화한다. */
public record GithubInstallationSyncRequestedEvent(long installationId) {
}
