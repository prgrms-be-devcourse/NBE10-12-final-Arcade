package com.back.domain.party.partyPr.service;

import com.back.domain.party.partyPr.dtos.PartyPrDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    private final Map<Long, Map<String, SseEmitter>> emittersByPartyId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(long partyId, List<PartyPrDto> snapshot) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        String emitterId = UUID.randomUUID().toString();
        emittersByPartyId.computeIfAbsent(partyId, ignored -> new ConcurrentHashMap<>()).put(emitterId, emitter);
        emitter.onCompletion(() -> remove(partyId, emitterId));
        emitter.onTimeout(() -> remove(partyId, emitterId));
        emitter.onError(ignored -> remove(partyId, emitterId));
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
            emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
        } catch (IOException | IllegalStateException exception) {
            remove(partyId, emitterId);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void publish(long partyId, PartyPrDto pullRequest) {
        Map<String, SseEmitter> emitters = emittersByPartyId.get(partyId);
        if (emitters == null) return;
        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().id(pullRequest.id() + ":" + pullRequest.githubUpdatedAt())
                        .name("pull-request").data(pullRequest));
            } catch (IOException | IllegalStateException exception) {
                log.debug("Party PR SSE emitter disconnected: partyId={}, emitterId={}", partyId, emitterId);
                remove(partyId, emitterId);
            }
        });
    }

    /** Party 종료 뒤 연결된 브라우저에 종료 사실을 알리고 stream을 닫는다. */
    public void completeParty(long partyId) {
        Map<String, SseEmitter> emitters = emittersByPartyId.remove(partyId);
        if (emitters == null) return;
        emitters.values().forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("party-complete").data("completed"));
            } catch (IOException | IllegalStateException ignored) {
                // 이미 끊긴 연결도 정상 종료 처리한다.
            }
            emitter.complete();
        });
    }

    private void remove(long partyId, String emitterId) {
        emittersByPartyId.computeIfPresent(partyId, (ignored, emitters) -> {
            emitters.remove(emitterId);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
