package com.back.domain.party.github.service;

import com.back.domain.party.github.entity.PartyGithubConnection;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 설치 처리 transaction이 롤백돼도 GitHub 연결 실패 상태는 별도 transaction으로 남긴다. */
@Service
@RequiredArgsConstructor
public class PartyGithubConnectionFailureService {
    private final PartyGithubConnectionRepository connectionRepository;
    private final PartyRepository partyRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markError(long partyId, String code, String message) {
        connection(partyId).markError(code, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markInstallationRequired(long partyId, String code, String message) {
        connection(partyId).markInstallationRequired(code, message);
    }

    private PartyGithubConnection connection(long partyId) {
        return connectionRepository.findByPartyId(partyId)
            .orElseGet(() -> {
                Party party = partyRepository.getReferenceById(partyId);
                // 연결 레코드는 repositoryFullName이 DB 필수값이다. 이 경로는 설치 실패를
                // 남기는 용도라 원본 URL을 보관하고, 정상 설치 시 owner/repository 값으로 갱신된다.
                String repository = party.getGithubRepoUrl() == null ? "" : party.getGithubRepoUrl();
                return connectionRepository.save(new PartyGithubConnection(party, repository));
            });
    }
}
