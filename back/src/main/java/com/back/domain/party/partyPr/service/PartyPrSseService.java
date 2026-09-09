package com.back.domain.party.partyPr.service;

import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.dtos.PartyPrByMemberDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Party별 PR 변경 스트림. 초기 snapshot을 보내므로 재접속 누락은 snapshot 재수신으로 복구한다. */
@Service
@Slf4j
public class PartyPrSseService {
    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final Map<Long, Map<String, Subscription>> allSubscriptionsByPartyId = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Subscription>> groupedSubscriptionsByPartyId = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Subscription>> memberSubscriptionsByPartyId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(long partyId, List<PartyPrDto> snapshot) {
        return subscribe(allSubscriptionsByPartyId, partyId, snapshot, SubscriptionType.ALL, null);
    }

    public SseEmitter subscribeGrouped(long partyId, List<PartyPrByMemberDto> snapshot) {
        return subscribe(groupedSubscriptionsByPartyId, partyId, snapshot, SubscriptionType.GROUPED, null);
    }

    public SseEmitter subscribeMember(long partyId, Long githubUserId, PartyPrByMemberDto snapshot) {
        return subscribe(memberSubscriptionsByPartyId, partyId, snapshot, SubscriptionType.MEMBER, githubUserId);
    }

    private SseEmitter subscribe(Map<Long, Map<String, Subscription>> store, long partyId, Object snapshot, SubscriptionType type, Long githubUserId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        String emitterId = UUID.randomUUID().toString();
        Subscription subscription = new Subscription(emitter, type, githubUserId);
        store.computeIfAbsent(partyId, ignored -> new ConcurrentHashMap<>())
                .put(emitterId, subscription);
        emitter.onCompletion(() -> remove(store, partyId, emitterId));
        emitter.onTimeout(() -> remove(store, partyId, emitterId));
        emitter.onError(ignored -> remove(store, partyId, emitterId));
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
            emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
        } catch (IOException | IllegalStateException exception) {
            remove(store, partyId, emitterId);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void publish(long partyId, PartyPrDto pullRequest) {
        send(allSubscriptionsByPartyId, allSubscriptionsByPartyId.get(partyId), partyId, pullRequest);
        Map<String, Subscription> members = memberSubscriptionsByPartyId.get(partyId);
        if (members == null) return;
        members.forEach((emitterId, subscription) -> {
            if (!subscription.acceptsPullRequest(pullRequest)) return;
            try {
                subscription.send(SseEmitter.event().id(pullRequest.id() + ":" + pullRequest.githubUpdatedAt())
                        .name("pull-request").data(pullRequest));
            } catch (IOException | IllegalStateException exception) {
                log.debug("Party PR SSE emitter disconnected: partyId={}, emitterId={}", partyId, emitterId);
                remove(memberSubscriptionsByPartyId, partyId, emitterId);
            }
        });
    }

    /** grouped stream에는 snapshot과 같은 그룹 DTO만 증분 이벤트로 보낸다. */
    public void publishGrouped(long partyId, PartyPrByMemberDto group) {
        Map<String, Subscription> subscriptions = groupedSubscriptionsByPartyId.get(partyId);
        if (subscriptions == null) return;
        subscriptions.forEach((emitterId, subscription) -> {
            if (subscription.type() != SubscriptionType.GROUPED) return;
            try {
                subscription.send(SseEmitter.event().name("pull-request-group").data(group));
            } catch (IOException | IllegalStateException exception) {
                log.debug("Party PR SSE emitter disconnected: partyId={}, emitterId={}", partyId, emitterId);
                remove(groupedSubscriptionsByPartyId, partyId, emitterId);
            }
        });
    }

    /** Party 종료 뒤 연결된 브라우저에 종료 사실을 알리고 stream을 닫는다. */
    public void completeParty(long partyId) {
        complete(allSubscriptionsByPartyId.remove(partyId));
        complete(groupedSubscriptionsByPartyId.remove(partyId));
        complete(memberSubscriptionsByPartyId.remove(partyId));
    }

    private void complete(Map<String, Subscription> subscriptions) {
        if (subscriptions == null) return;
        subscriptions.values().forEach(subscription -> {
            try {
                subscription.send(SseEmitter.event().name("party-complete").data("completed"));
            } catch (IOException | IllegalStateException ignored) {
                // 이미 끊긴 연결도 정상 종료 처리한다.
            }
            subscription.emitter().complete();
        });
    }

    @Scheduled(fixedDelayString = "${custom.party-pr.sse.heartbeat-millis:30000}")
    public void heartbeat() {
        heartbeat(allSubscriptionsByPartyId); heartbeat(groupedSubscriptionsByPartyId); heartbeat(memberSubscriptionsByPartyId);
    }

    private void heartbeat(Map<Long, Map<String, Subscription>> store) {
        store.forEach((partyId, subscriptions) -> subscriptions.forEach((id, subscription) -> {
            try { subscription.send(SseEmitter.event().name("heartbeat").data("ping")); }
            catch (IOException | IllegalStateException exception) { remove(store, partyId, id); }
        }));
    }

    private void send(Map<Long, Map<String, Subscription>> store, Map<String, Subscription> subscriptions, long partyId, PartyPrDto pullRequest) {
        if (subscriptions == null) return;
        subscriptions.forEach((id, subscription) -> { try { subscription.send(SseEmitter.event().id(pullRequest.id()+":"+pullRequest.githubUpdatedAt()).name("pull-request").data(pullRequest)); }
            catch (IOException | IllegalStateException exception) { remove(store, partyId, id); } });
    }

    private void remove(Map<Long, Map<String, Subscription>> store, long partyId, String emitterId) {
        store.computeIfPresent(partyId, (ignored, subscriptions) -> {
            subscriptions.remove(emitterId);
            return subscriptions.isEmpty() ? null : subscriptions;
        });
    }

    private enum SubscriptionType { ALL, GROUPED, MEMBER }

    private record Subscription(SseEmitter emitter, SubscriptionType type, Long githubUserId) {
        private synchronized void send(SseEmitter.SseEventBuilder event) throws IOException {
            emitter.send(event);
        }
        private boolean acceptsPullRequest(PartyPrDto pullRequest) {
            if (type == SubscriptionType.GROUPED) return false;
            if (type == SubscriptionType.ALL) return true;
            return githubUserId != null && githubUserId.equals(pullRequest.authorGithubUserId());
        }
    }
}
