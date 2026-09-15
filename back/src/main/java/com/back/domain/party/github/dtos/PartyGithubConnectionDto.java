package com.back.domain.party.github.dtos;

import com.back.domain.party.github.entity.PartyGithubConnection;

public record PartyGithubConnectionDto(
        String status,
        String repositoryFullName,
        String lastErrorCode,
        String lastError
) {

    public PartyGithubConnectionDto(PartyGithubConnection connection) {
        this(connection.getStatus().name(), connection.getRepositoryFullName(), connection.getLastErrorCode(), connection.getLastError());
    }

}
