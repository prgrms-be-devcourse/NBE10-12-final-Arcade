package com.back.global.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

/**
 * 다음 페이지 유무만 알려주는 목록 응답. 더보기·무한스크롤처럼 전체 개수를 쓰지 않는 화면에 쓴다.
 *
 * PageDto 와 달리 count 쿼리를 돌리지 않아 totalElements·totalPages 를 줄 수 없다.
 * 전체 개수를 화면에 표시해야 하면 PageDto 쪽이다.
 */
public record SliceDto<T>(
        List<T> content,
        int page,
        int size,
        boolean hasNext
) {
    public SliceDto(Slice<T> slice) {
        this(slice.getContent(), slice.getNumber(), slice.getSize(), slice.hasNext());
    }
}
