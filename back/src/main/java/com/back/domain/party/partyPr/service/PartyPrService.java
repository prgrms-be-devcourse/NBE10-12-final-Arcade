package com.back.domain.party.partyPr.service;

import com.back.domain.party.github.entity.PartyGithubConnectionStatus;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import com.back.global.github.client.dtos.GithubPullRequestResponse;
import com.back.global.github.event.GithubPullRequestReceivedEvent;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.partyPr.model.GithubPullRequestSnapshot;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/*
  GitHub pull_request webhook과 최초 목록 조회 결과를 PartyPr로 수렴시키는 서비스다.
  GitHub App 공통 webhook의 원본 body만 처리한다.
 */

public class PartyPrService {
    private final PartyGithubConnectionRepository githubConnectionRepository;
    private final PartyPrRepository partyPrRepository;

    public List<PartyPrDto> getByPartyId(long partyId) {
        return partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(partyId)
            .stream().map(PartyPrDto::new).toList();
    }

    @Transactional
    @EventListener
    public int receivePullRequestEvent(GithubPullRequestReceivedEvent event) {
        // repository 이름은 rename될 수 있어 routing에는 불변인 repository.id만 사용한다.
        long repositoryId = requiredLong(event.repositoryId(), "repository.id");
        long installationId = requiredLong(event.installationId(), "installation.id");
        GithubPullRequestSnapshot data = toSnapshot(event.pullRequest());

        var connections = githubConnectionRepository
            .findAllByRepositoryIdAndInstallationIdAndStatus(repositoryId, installationId, PartyGithubConnectionStatus.ACTIVE)
            .stream().toList();

        for (var connection : connections) {
            long partyId = connection.getParty().getId();
            upsert(connection.getParty(), data);
        }

        return connections.size();
    }

    /** GitHub App installation token으로 조회한 기존 PR을 반영한다. */
    @Transactional
    public void syncExistingPullRequests(Party party, List<GithubPullRequestResponse> pullRequests) {
        for (GithubPullRequestResponse pullRequest : pullRequests) {
            upsert(party, toSnapshot(pullRequest));
        }
    }

    private void upsert(Party party, GithubPullRequestSnapshot data) {
        PartyPr partyPr = partyPrRepository
                .findByPartyIdAndGithubPrId(party.getId(), data.githubPrId())
                .orElseGet(() -> new PartyPr(party, data));
        // 초기 sync와 webhook이 경합해도 더 오래된 GitHub 상태가 최신 상태를 덮어쓰지 못하게 한다.
        if (partyPr.getId() != null && partyPr.getGithubUpdatedAt() != null && data.githubUpdatedAt() != null
            && data.githubUpdatedAt().isBefore(partyPr.getGithubUpdatedAt())) return;
        partyPr.update(data);
        partyPrRepository.save(partyPr);
    }

    /** GitHub 외부 DTO를 PartyPr이 이해하는 내부 snapshot으로 변환하면서 필수 필드를 검증한다. */
    private GithubPullRequestSnapshot toSnapshot(GithubPullRequestResponse pr) {
        if (pr == null) throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다: pull_request");
        return new GithubPullRequestSnapshot(
            requiredLong(pr.id(), "id"), requiredInt(pr.number(), "number"), requiredText(pr.title(), "title"),
            requiredText(pr.htmlUrl(), "html_url"), requiredText(pr.state(), "state"),
            pr.user() == null ? null : pr.user().login(),
            Boolean.TRUE.equals(pr.draft()),
            Boolean.TRUE.equals(pr.merged()) || pr.mergedAt() != null,
            pr.base() == null ? null : pr.base().ref(), pr.head() == null ? null : pr.head().ref(),
            date(pr.createdAt()), date(pr.closedAt()), date(pr.mergedAt()), date(pr.updatedAt())
        );
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다: " + field);
        return value;
    }

    private long requiredLong(long value, String field) {
        if (value <= 0) {
            throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다: " + field);
        }
        return value;
    }

    private int requiredInt(int value, String field) {
        if (value <= 0) {
            throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다: " + field);
        }
        return value;
    }

    private OffsetDateTime date(String value) {
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
    }
}
