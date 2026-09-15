package com.back.domain.party.github.service;

import com.back.domain.party.github.entity.PartyGithubBindingAudit;
import com.back.domain.party.github.entity.PartyGithubBindingAuditAction;
import com.back.domain.party.github.entity.PartyGithubBindingStatus;
import com.back.domain.party.github.repository.PartyGithubBindingAuditRepository;
import com.back.domain.party.github.repository.PartyGithubBindingRepository;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** AFTER_COMMIT listener가 별도 트랜잭션으로 Party GitHub 연결 상태를 안전하게 archive하도록 분리한다. */
@Service
@RequiredArgsConstructor
public class PartyGithubBindingArchiveService {
    private final PartyGithubBindingRepository bindingRepository;
    private final PartyGithubBindingAuditRepository auditRepository;
    private final PartyGithubConnectionRepository legacyConnectionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void archivePartyConnection(long partyId) {
        bindingRepository.findByPartyIdAndStatus(partyId, PartyGithubBindingStatus.ACTIVE).ifPresent(binding -> {
            binding.archive();
            auditRepository.save(new PartyGithubBindingAudit(binding.getParty(), binding.getInstallationRepository(), null,
                    PartyGithubBindingAuditAction.ARCHIVED, "PARTY_COMPLETED"));
        });
        legacyConnectionRepository.findByPartyId(partyId).ifPresent(connection -> connection.archive());
    }
}
