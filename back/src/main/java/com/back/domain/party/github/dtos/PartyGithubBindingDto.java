package com.back.domain.party.github.dtos;

import com.back.domain.party.github.entity.PartyGithubBinding;

import java.time.LocalDateTime;

public record PartyGithubBindingDto(long id, long repositoryId, String repositoryFullName,
                                    String status, LocalDateTime linkedAt, LocalDateTime disconnectedAt) {
    public PartyGithubBindingDto(PartyGithubBinding binding) {
        this(binding.getId(), binding.getInstallationRepository().getRepositoryId(),
                binding.getInstallationRepository().getFullName(), binding.getStatus().name(),
                binding.getLinkedAt(), binding.getDisconnectedAt());
    }
}
