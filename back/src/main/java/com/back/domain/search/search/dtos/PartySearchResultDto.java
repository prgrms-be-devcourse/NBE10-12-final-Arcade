package com.back.domain.search.search.dtos;

import com.back.domain.party.party.dtos.PartyListItemDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record PartySearchResultDto(
        String query,
        List<String> matchedKeywords,
        List<PartyListItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PartySearchResultDto(String query, List<String> matchedKeywords, Page<PartyListItemDto> page) {
        this(
                query,
                matchedKeywords,
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
