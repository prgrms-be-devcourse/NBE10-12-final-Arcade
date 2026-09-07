package com.back.domain.party.github.dtos;

/** 현재 GitHub App user token으로 접근 가능한 installation 레포만 외부에 노출한다. */
public record GithubConnectableRepositoryDto(long installationRepositoryId, long installationId,
                                             long repositoryId, String fullName, String htmlUrl) {
}
