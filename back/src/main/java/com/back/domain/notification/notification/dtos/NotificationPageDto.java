package com.back.domain.notification.notification.dtos;

import org.springframework.data.domain.Page;

import java.util.List;

public record NotificationPageDto(
        List<NotificationDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public NotificationPageDto(Page<NotificationDto> page) {
        this(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
