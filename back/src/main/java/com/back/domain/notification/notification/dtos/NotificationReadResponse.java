package com.back.domain.notification.notification.dtos;

import com.back.domain.notification.notification.entity.Notification;

public record NotificationReadResponse(
        long id,
        boolean isRead
) {
    public NotificationReadResponse(Notification notification) {
        this(notification.getId(), notification.isRead());
    }
}
