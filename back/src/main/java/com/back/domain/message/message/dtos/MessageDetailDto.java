package com.back.domain.message.message.dtos;

import com.back.domain.message.message.entity.Message;

import java.time.LocalDateTime;

public record MessageDetailDto(
        long id,
        MessageMemberDto sender,
        MessageMemberDto recipient,
        String content,
        boolean isRead,
        LocalDateTime createAt)
{
    public MessageDetailDto(
            Message message,
            MessageMemberDto sender,
            MessageMemberDto recipient) {
        this(
                message.getId(),
                sender,
                recipient,
                message.getContent(),
                message.isRead(),
                message.getCreateDate());
    }
}
