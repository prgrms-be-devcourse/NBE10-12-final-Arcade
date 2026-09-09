package com.back.domain.party.partyPr.service;

import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.github.entity.PartyGithubConnectionStatus;
import com.back.domain.party.github.entity.PartyGithubBindingStatus;
import com.back.domain.party.github.repository.GithubInstallationRepositoryRepository;
import com.back.domain.party.github.repository.PartyGithubConnectionRepository;
import com.back.domain.party.github.repository.PartyGithubBindingRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.global.github.client.dtos.GithubPullRequestResponse;
import com.back.global.github.event.GithubPullRequestReceivedEvent;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.dtos.PartyPrByMemberDto;
import com.back.domain.party.partyPr.entity.PartyPr;
import com.back.domain.party.partyPr.model.GithubPullRequestSnapshot;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/*
  GitHub pull_request webhook과 최초 목록 조회 결과를 PartyPr로 수렴시키는 서비스다.
  GitHub App 공통 webhook의 원본 body만 처리한다.
 */

public class PartyPrService {
    private final PartyGithubConnectionRepository githubConnectionRepository;
    private final GithubInstallationRepositoryRepository installationRepositoryRepository;
    private final PartyGithubBindingRepository bindingRepository;
    private final PartyPrRepository partyPrRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyPrSseService partyPrSseService;

    public List<PartyPrDto> getByPartyId(long partyId, Member actor) {
        Party party = party(partyId);
        ensureReadable(party, actor);
        return partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(partyId)
            .stream().map(PartyPrDto::new).toList();
    }

    /**
     * 파티장과 승인된 파티원을 먼저 응답에 포함하고, 불변 GitHub user id로 PR 작성자를 연결한다.
     * 파티원과 연결되지 않은 작성자의 PR도 외부 작성자 그룹으로 보존한다.
     */
    public List<PartyPrByMemberDto> getByPartyIdGroupedByMember(long partyId, Member actor) {
        Party party = party(partyId);
        ensureReadable(party, actor);

        var members = new LinkedHashMap<Long, Member>();
        members.put(party.getOwner().getId(), party.getOwner());
        partyMemberRepository.findAllByParty(party).stream()
                .filter(partyMember -> partyMember.getStatus() == PartyMemberStatus.APPROVED)
                .forEach(partyMember -> members.put(partyMember.getMember().getId(), partyMember.getMember()));

        Map<Long, List<PartyPrDto>> pullRequestsByGithubUserId = new LinkedHashMap<>();
        List<PartyPrDto> unknownAuthorPullRequests = new ArrayList<>();
        Map<Long, String> githubLoginByUserId = new LinkedHashMap<>();
        for (PartyPr pullRequest : partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(partyId)) {
            PartyPrDto dto = new PartyPrDto(pullRequest);
            Long githubUserId = pullRequest.getAuthorGithubUserId();
            if (githubUserId == null) {
                unknownAuthorPullRequests.add(dto);
                continue;
            }
            pullRequestsByGithubUserId.computeIfAbsent(githubUserId, ignored -> new ArrayList<>()).add(dto);
            githubLoginByUserId.putIfAbsent(githubUserId, pullRequest.getAuthorLogin());
        }

        List<PartyPrByMemberDto> result = new ArrayList<>();
        Set<Long> matchedGithubUserIds = new HashSet<>();
        for (Member member : members.values()) {
            Long githubUserId = member.getGithubUserId();
            List<PartyPrDto> pullRequests = githubUserId == null
                    ? List.of()
                    : pullRequestsByGithubUserId.getOrDefault(githubUserId, List.of());
            if (githubUserId != null) matchedGithubUserIds.add(githubUserId);
            result.add(PartyPrByMemberDto.member(member, party.isOwnedBy(member), pullRequests));
        }

        pullRequestsByGithubUserId.forEach((githubUserId, pullRequests) -> {
            if (!matchedGithubUserIds.contains(githubUserId)) {
                result.add(PartyPrByMemberDto.external(
                        githubUserId, githubLoginByUserId.get(githubUserId), pullRequests
                ));
            }
        });
        if (!unknownAuthorPullRequests.isEmpty()) {
            result.add(PartyPrByMemberDto.external(null, null, unknownAuthorPullRequests));
        }
        return result;
    }

    public PartyPrByMemberDto getByPartyIdAndMemberId(long partyId, long memberId, Member actor) {
        Party party = party(partyId);
        ensureReadable(party, actor);

        Member member = party.getOwner().getId() == memberId
                ? party.getOwner()
                : partyMemberRepository
                .findByParty_IdAndMember_IdAndStatus(partyId, memberId, PartyMemberStatus.APPROVED)
                .map(PartyMember::getMember)
                .orElseThrow(() -> new ServiceException(
                        "404-2", "파티장 또는 승인된 파티원을 찾을 수 없습니다."
                ));

        List<PartyPrDto> pullRequests = member.getGithubUserId() == null
                ? List.of()
                : partyPrRepository
                        .findAllByPartyIdAndAuthorGithubUserIdOrderByGithubUpdatedAtDesc(
                                partyId, member.getGithubUserId()
                        ).stream()
                        .map(PartyPrDto::new)
                        .toList();

        return PartyPrByMemberDto.member(member, party.isOwnedBy(member), pullRequests);
    }

    public List<PartyPrDto> getMyPullRequests(Member actor) {
        if (actor == null || actor.getGithubUserId() == null) {
            throw new ServiceException("400-20", "GITHUB_ACCOUNT_LINK_REQUIRED");
        }
        return partyPrRepository.findAllByAuthorGithubUserIdOrderByGithubUpdatedAtDesc(actor.getGithubUserId()).stream()
                .filter(pullRequest -> canRead(pullRequest.getParty(), actor))
                .map(PartyPrDto::new).toList();
    }

    @Transactional
    @EventListener
    public int receivePullRequestEvent(GithubPullRequestReceivedEvent event) {
        // repository 이름은 rename될 수 있어 routing에는 불변인 repository.id만 사용한다.
        long repositoryId = requiredLong(event.repositoryId(), "repository.id");
        long installationId = requiredLong(event.installationId(), "installation.id");
        GithubPullRequestSnapshot data = toSnapshot(event.pullRequest());

        var parties = new LinkedHashMap<Long, Party>();
        githubConnectionRepository
            .findAllByRepositoryIdAndInstallationIdAndStatus(repositoryId, installationId, PartyGithubConnectionStatus.ACTIVE)
            .forEach(connection -> parties.put(connection.getParty().getId(), connection.getParty()));
        // 신규 binding 기반 연결과 기존 PartyGithubConnection을 함께 지원해 데이터 이행 중 webhook 누락을 막는다.
        installationRepositoryRepository.findByInstallationInstallationIdAndRepositoryId(installationId, repositoryId)
                .ifPresent(repository -> bindingRepository
                        .findAllByInstallationRepositoryIdAndStatus(repository.getId(), PartyGithubBindingStatus.ACTIVE)
                        .forEach(binding -> parties.put(binding.getParty().getId(), binding.getParty())));

        for (Party party : parties.values()) {
            upsert(party, data);
        }

        return parties.size();
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
        publishAfterCommit(party.getId(), new PartyPrDto(partyPr));
    }

    /** GitHub 외부 DTO를 PartyPr이 이해하는 내부 snapshot으로 변환하면서 필수 필드를 검증한다. */
    private GithubPullRequestSnapshot toSnapshot(GithubPullRequestResponse pr) {
        if (pr == null) throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다: pull_request");
        return new GithubPullRequestSnapshot(
            requiredLong(pr.id(), "id"), requiredInt(pr.number(), "number"), requiredText(pr.title(), "title"),
            requiredText(pr.htmlUrl(), "html_url"), requiredText(pr.state(), "state"),
            pr.user() == null || pr.user().id() <= 0 ? null : pr.user().id(),
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

    private Party party(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "파티를 찾을 수 없습니다."));
    }

    private void ensureReadable(Party party, Member actor) {
        if (!canRead(party, actor)) throw new ServiceException("403-1", "파티장 또는 확정 파티원만 PR을 조회할 수 있습니다.");
    }

    private boolean canRead(Party party, Member actor) {
        return actor != null && (party.isOwnedBy(actor)
                || partyMemberRepository.existsByPartyAndMemberAndStatus(party, actor, PartyMemberStatus.APPROVED));
    }

    private void publishAfterCommit(long partyId, PartyPrDto pullRequest) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            partyPrSseService.publish(partyId, pullRequest);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                partyPrSseService.publish(partyId, pullRequest);
            }
        });
    }
}
