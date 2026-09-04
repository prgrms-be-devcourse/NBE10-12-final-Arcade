package com.back.domain.notification.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationSseEventListener {
    private final NotificationRedisPublisher notificationRedisPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(NotificationCreatedEvent event) {
        notificationRedisPublisher.publish(event);
    }
}
