package com.back.domain.party.github.dtos;

import jakarta.validation.constraints.NotBlank;

public record PartyGithubRepositoryUrlRq(@NotBlank String githubRepoUrl) {
}
