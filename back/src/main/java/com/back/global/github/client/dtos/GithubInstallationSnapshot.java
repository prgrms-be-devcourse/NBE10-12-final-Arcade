package com.back.global.github.client.dtos;

import com.back.global.github.client.GithubAppClient;
import java.util.List;

/** 트랜잭션 밖에서 조회한 설치와 선택 저장소의 일관된 반영 단위. */
public record GithubInstallationSnapshot(GithubAppClient.Installation installation,
                                         List<GithubAppClient.Repository> repositories) {
    public GithubInstallationSnapshot {
        repositories = List.copyOf(repositories);
    }
}
