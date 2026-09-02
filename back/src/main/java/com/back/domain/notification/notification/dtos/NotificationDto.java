package com.back.domain.notification.notification.dtos;

import com.back.domain.notification.notification.entity.Notification;
import com.back.domain.notification.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        long id,
        NotificationType type,
        String content,
        boolean isRead,
        LocalDateTime createAt
) {
    public NotificationDto(Notification notification) {
        this(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.isRead(),
                notification.getCreateDate()
        );
    }
}
