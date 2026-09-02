package com.back.domain.message.message.dtos;

import org.springframework.data.domain.Page;

import java.util.List;

public record MessagePageDto(
        List<MessageListDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public MessagePageDto(Page<MessageListDto> page) {
        this(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
