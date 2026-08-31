package com.back.domain.party.github.entity;

/** GitHub App 설치 및 PR 동기화의 완료 여부를 표현한다. */
public enum PartyGithubConnectionStatus {
    PENDING, SYNCING, ACTIVE, INSTALLATION_REQUIRED, APPROVAL_PENDING, ERROR
}
