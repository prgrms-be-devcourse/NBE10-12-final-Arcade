package com.back.global.github.event;

import com.back.global.github.client.dtos.GithubInstallationSnapshot;

/** 외부 조회가 끝난 설치 snapshot을 동일 웹훅 트랜잭션에 반영한다. */
public record GithubInstallationSyncRequestedEvent(GithubInstallationSnapshot snapshot) {
}
