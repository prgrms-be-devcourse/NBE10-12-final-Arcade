package com.back.domain.notification.notification.service;

import com.back.domain.notification.notification.dtos.NotificationDto;
import com.back.domain.notification.notification.entity.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationRedisPubSubTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private NotificationSseService notificationSseService;

    @Mock
    private NotificationRedisPublisher notificationRedisPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Redis 발행: 알림 생성 이벤트를 notification.created 채널에 JSON으로 발행한다")
    void publish() throws Exception {
        NotificationCreatedEvent event = event();
        NotificationRedisPublisher publisher = new NotificationRedisPublisher(redisTemplate, objectMapper);

        publisher.publish(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(NotificationRedisPublisher.CHANNEL), payloadCaptor.capture());
        assertThat(objectMapper.readValue(payloadCaptor.getValue(), NotificationCreatedEvent.class)).isEqualTo(event);
    }

    @Test
    @DisplayName("Redis 수신: 수신한 알림 이벤트를 해당 회원의 로컬 SSE 연결에 전달한다")
    void subscribe() throws Exception {
        NotificationCreatedEvent event = event();
        NotificationRedisSubscriber subscriber = new NotificationRedisSubscriber(objectMapper, notificationSseService);
        DefaultMessage message = new DefaultMessage(
                NotificationRedisPublisher.CHANNEL.getBytes(StandardCharsets.UTF_8),
                objectMapper.writeValueAsBytes(event)
        );

        subscriber.onMessage(message, null);

        verify(notificationSseService).send(event.memberId(), event.notification());
    }

    @Test
    @DisplayName("커밋 후 이벤트 수신: Redis 발행기로 알림 생성 이벤트를 전달한다")
    void sendAfterCommit() {
        NotificationCreatedEvent event = event();
        NotificationSseEventListener listener = new NotificationSseEventListener(notificationRedisPublisher);

        listener.send(event);

        verify(notificationRedisPublisher).publish(event);
    }

    private NotificationCreatedEvent event() {
        return new NotificationCreatedEvent(
                1L,
                new NotificationDto(2L, NotificationType.PARTY_APPLICATION_APPROVED, "승인되었습니다.", false, LocalDateTime.of(2026, 9, 4, 9, 0))
        );
    }
}
