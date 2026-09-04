package com.back.domain.notification.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final NotificationSseService notificationSseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        NotificationCreatedEvent event = objectMapper.readValue(message.getBody(), NotificationCreatedEvent.class);
        notificationSseService.send(event.memberId(), event.notification());
    }
}
