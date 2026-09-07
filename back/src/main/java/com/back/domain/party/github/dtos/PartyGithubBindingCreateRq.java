package com.back.domain.party.github.dtos;

import jakarta.validation.constraints.NotNull;

public record PartyGithubBindingCreateRq(@NotNull Long installationRepositoryId) {
}
