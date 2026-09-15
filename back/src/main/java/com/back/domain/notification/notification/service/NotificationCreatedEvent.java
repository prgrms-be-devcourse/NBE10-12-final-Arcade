package com.back.domain.notification.notification.service;

import com.back.domain.notification.notification.dtos.NotificationDto;

public record NotificationCreatedEvent(long memberId, NotificationDto notification) {
}
