package com.back.domain.notification.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisPublisher {
    public static final String CHANNEL = "notification.created";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(NotificationCreatedEvent event) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException | RuntimeException exception) {
            // DB 알림은 이미 커밋됐다. Redis 장애로 API 요청 자체가 실패하지 않도록 하고,
            // 클라이언트는 목록 조회 또는 SSE 재연결 후 누락 알림을 확인한다.
            log.error("Failed to publish notification SSE event: memberId={}", event.memberId(), exception);
        }
    }
}
