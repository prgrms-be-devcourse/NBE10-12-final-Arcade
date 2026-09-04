package com.back.domain.notification.notification.service;

import com.back.domain.notification.notification.dtos.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationSseService {
    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<Long, Map<String, SseEmitter>> emittersByMemberId = new ConcurrentHashMap<>();

    /**
     * 한 회원은 여러 브라우저 탭에서 동시에 구독할 수 있다.
     */
    public SseEmitter subscribe(long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        String emitterId = UUID.randomUUID().toString();

        emittersByMemberId
                .computeIfAbsent(memberId, ignored -> new ConcurrentHashMap<>())
                .put(emitterId, emitter);

        emitter.onCompletion(() -> remove(memberId, emitterId));
        emitter.onTimeout(() -> remove(memberId, emitterId));
        emitter.onError(ignored -> remove(memberId, emitterId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException exception) {
            remove(memberId, emitterId);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void send(long memberId, NotificationDto notification) {
        Map<String, SseEmitter> emitters = emittersByMemberId.get(memberId);
        if (emitters == null) {
            return;
        }

        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.id()))
                        .name("notification")
                        .data(notification));
            } catch (IOException | IllegalStateException exception) {
                log.debug("SSE emitter disconnected: memberId={}, emitterId={}", memberId, emitterId);
                remove(memberId, emitterId);
            }
        });
    }

    private void remove(long memberId, String emitterId) {
        emittersByMemberId.computeIfPresent(memberId, (ignored, emitters) -> {
            emitters.remove(emitterId);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
