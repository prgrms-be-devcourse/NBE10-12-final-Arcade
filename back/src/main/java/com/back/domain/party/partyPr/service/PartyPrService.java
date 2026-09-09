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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    // 같은 JVM에서 initial sync와 webhook이 동시에 같은 신규 PR을 INSERT하는 경합을 막는다.
    // DB unique constraint는 다중 인스턴스 환경의 최종 안전망으로 유지한다.
    private final Map<String, InsertLock> insertLocks = new ConcurrentHashMap<>();

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
        return groupedByMember(party);
    }

    private List<PartyPrByMemberDto> groupedByMember(Party party) {
        var members = new LinkedHashMap<Long, Member>();
        members.put(party.getOwner().getId(), party.getOwner());
        partyMemberRepository.findAllByParty(party).stream()
                .filter(partyMember -> partyMember.getStatus() == PartyMemberStatus.APPROVED)
                .forEach(partyMember -> members.put(partyMember.getMember().getId(), partyMember.getMember()));

        Map<Long, List<PartyPrDto>> pullRequestsByGithubUserId = new LinkedHashMap<>();
        List<PartyPrDto> unknownAuthorPullRequests = new ArrayList<>();
        Map<Long, String> githubLoginByUserId = new LinkedHashMap<>();
        for (PartyPr pullRequest : partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(party.getId())) {
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
            result.add(PartyPrByMemberDto.member(
                    member, party.isOwnedBy(member),
                    githubUserId == null ? null : githubLoginByUserId.get(githubUserId), pullRequests
            ));
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

        String githubLogin = pullRequests.isEmpty() ? null : pullRequests.getFirst().authorLogin();
        return PartyPrByMemberDto.member(member, party.isOwnedBy(member), githubLogin, pullRequests);
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
        List<GithubPullRequestSnapshot> snapshots = pullRequests.stream().map(this::toSnapshot).toList();
        if (snapshots.isEmpty()) return;

        Map<Long, PartyPr> existingByGithubPrId = partyPrRepository
                .findAllByPartyIdAndGithubPrIdIn(party.getId(), snapshots.stream()
                        .map(GithubPullRequestSnapshot::githubPrId).toList())
                .stream().collect(Collectors.toMap(PartyPr::getGithubPrId, Function.identity()));

        for (GithubPullRequestSnapshot snapshot : snapshots) {
            PartyPr existing = existingByGithubPrId.get(snapshot.githubPrId());
            if (existing == null) {
                // 신규 PR은 webhook과 경합할 수 있으므로 기존 upsert 경로를 사용한다.
                upsert(party, snapshot);
                continue;
            }
            updateExisting(party, existing, snapshot);
        }
    }

    private void updateExisting(Party party, PartyPr partyPr, GithubPullRequestSnapshot data) {
        if (data.githubUpdatedAt().isBefore(partyPr.getGithubUpdatedAt()) || partyPr.hasSameContent(data)) return;
        partyPr.update(data);
        PartyPrDto pullRequest = new PartyPrDto(partyPr);
        publishAfterCommit(party.getId(), pullRequest, groupFor(party, pullRequest));
    }

    private void upsert(Party party, GithubPullRequestSnapshot data) {
        String key = party.getId() + ":" + data.githubPrId();
        InsertLock lock = insertLocks.compute(key, (ignored, current) -> {
            InsertLock acquired = current == null ? new InsertLock() : current;
            acquired.users++;
            return acquired;
        });
        lock.lock.lock();
        try {
            PartyPr partyPr = partyPrRepository
                    .findByPartyIdAndGithubPrId(party.getId(), data.githubPrId())
                    .orElseGet(() -> new PartyPr(party, data));
            // 초기 sync와 webhook이 경합해도 더 오래된 GitHub 상태가 최신 상태를 덮어쓰지 못하게 한다.
            if (partyPr.getId() != null && data.githubUpdatedAt().isBefore(partyPr.getGithubUpdatedAt())) return;
            if (partyPr.getId() != null && partyPr.hasSameContent(data)) return;
            partyPr.update(data);
            partyPrRepository.save(partyPr);
            PartyPrDto pullRequest = new PartyPrDto(partyPr);
            PartyPrByMemberDto group = groupFor(party, pullRequest);
            publishAfterCommit(party.getId(), pullRequest, group);
        } finally { releaseAfterTransaction(key, lock); }
    }

    private void releaseAfterTransaction(String key, InsertLock lock) {
        Runnable release = () -> {
            lock.lock.unlock();
            insertLocks.compute(key, (ignored, current) -> current != lock ? current : --lock.users == 0 ? null : lock);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { release.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { release.run(); }
        });
    }

    private static final class InsertLock {
        private final ReentrantLock lock = new ReentrantLock();
        private int users;
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
            date(pr.createdAt()), date(pr.closedAt()), date(pr.mergedAt()), requiredDate(pr.updatedAt(), "updated_at")
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

    private OffsetDateTime requiredDate(String value, String field) {
        OffsetDateTime parsed = date(value);
        if (parsed == null) {
            throw new ServiceException("400-2", "GitHub 웹훅 필수 값이 없습니다: " + field);
        }
        return parsed;
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

    private PartyPrByMemberDto groupFor(Party party, PartyPrDto pullRequest) {
        Long githubUserId = pullRequest.authorGithubUserId();
        List<PartyPrDto> pullRequests = githubUserId == null
                ? partyPrRepository.findAllByPartyIdAndAuthorGithubUserIdIsNullOrderByGithubUpdatedAtDesc(party.getId())
                        .stream().map(PartyPrDto::new).toList()
                : partyPrRepository.findAllByPartyIdAndAuthorGithubUserIdOrderByGithubUpdatedAtDesc(
                        party.getId(), githubUserId
                ).stream().map(PartyPrDto::new).toList();

        if (githubUserId == null) {
            return PartyPrByMemberDto.external(null, null, pullRequests);
        }

        String githubLogin = pullRequests.isEmpty() ? pullRequest.authorLogin() : pullRequests.getFirst().authorLogin();
        Member member = memberForGithubUserId(party, githubUserId);
        return member == null
                ? PartyPrByMemberDto.external(githubUserId, githubLogin, pullRequests)
                : PartyPrByMemberDto.member(member, party.isOwnedBy(member), githubLogin, pullRequests);
    }

    /**
     * SSE 증분 이벤트는 변경된 작성자 그룹만 갱신하면 된다.
     * 전체 파티원/PR 목록을 다시 그룹화하지 않도록 작성자와 승인 멤버만 조회한다.
     */
    private Member memberForGithubUserId(Party party, Long githubUserId) {
        if (githubUserId.equals(party.getOwner().getGithubUserId())) {
            return party.getOwner();
        }
        return partyMemberRepository
                .findByPartyAndMember_GithubUserIdAndStatus(party, githubUserId, PartyMemberStatus.APPROVED)
                .map(PartyMember::getMember)
                .orElse(null);
    }

    private void publishAfterCommit(long partyId, PartyPrDto pullRequest, PartyPrByMemberDto group) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            partyPrSseService.publish(partyId, pullRequest);
            partyPrSseService.publishGrouped(partyId, group);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                partyPrSseService.publish(partyId, pullRequest);
                partyPrSseService.publishGrouped(partyId, group);
            }
        });
    }
}
