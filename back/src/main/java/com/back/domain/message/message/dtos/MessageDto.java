package com.back.domain.message.message.dtos;

import com.back.domain.message.message.entity.Message;

import java.time.LocalDateTime;

public record MessageDto(
        long id,
        long senderId,
        long recipientId,
        String content,
        boolean isRead,
        LocalDateTime createAt
) {
    public MessageDto(Message message) {
        this(
                message.getId(),
                message.getSender().getId(),
                message.getRecipient().getId(),
                message.getContent(),
                message.isRead(),
                message.getCreateDate()
        );
    }
}
