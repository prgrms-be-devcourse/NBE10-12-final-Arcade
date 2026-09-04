package com.back.domain.notification.notification.service;

import com.back.standard.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisSubscriber implements MessageListener {
    private final NotificationSseService notificationSseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        NotificationCreatedEvent event = Util.json.fromBytes(message.getBody(), NotificationCreatedEvent.class);
        if (event == null) {
            log.error("Failed to deserialize notification Redis message");
            return;
        }

        notificationSseService.send(event.memberId(), event.notification());
    }
}
