package com.back.domain.message.message.dtos;

import com.back.domain.message.message.entity.Message;

import java.time.LocalDateTime;

public record MessageListDto(
        long id,
        MessageMemberDto sender,
        MessageMemberDto recipient,
        String contentPreview,
        boolean isRead,
        LocalDateTime createAt) {

    public MessageListDto(
            Message message,
            MessageMemberDto sender,
            MessageMemberDto recipient) {
        this(
                message.getId(),
                sender,
                recipient,
                message.getContent(),
                message.isRead(),
                message.getCreateDate()
        );
    }
}
