package com.back.domain.party.github.dtos;

import java.time.LocalDateTime;

public record GithubAppUserAuthorizationStatusDto(boolean githubSocialLinked, boolean authorized,
                                                   boolean expired, LocalDateTime accessTokenExpiresAt) {
}
