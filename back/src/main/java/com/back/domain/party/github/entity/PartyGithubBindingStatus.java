package com.back.domain.party.github.entity;

/** Party 단위의 논리 연결 상태. App installation 자체의 상태와 구분한다. */
public enum PartyGithubBindingStatus {
    SYNCING, ACTIVE, ARCHIVED, DISCONNECTED
}
