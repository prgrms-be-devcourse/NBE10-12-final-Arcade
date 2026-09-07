package com.back.global.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지 응답 공통 껍데기. 목록 응답은 이걸로 감싼다.
 *
 * Page<T>를 그대로 내보내면 page가 아니라 number로 나가고 pageable·sort·first·last 같은
 * 내부 표현이 함께 실려서, 프론트와 합의한 {content, page, size, totalElements, totalPages}가 아니다.
 * 도메인마다 이 레코드를 복사해 두던 것(MessagePageDto·NotificationPageDto)을 여기로 합쳤다 -
 * 필드명·순서가 같아 JSON 출력은 그대로다.
 */
public record PageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PageDto(Page<T> page) {
        this(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
