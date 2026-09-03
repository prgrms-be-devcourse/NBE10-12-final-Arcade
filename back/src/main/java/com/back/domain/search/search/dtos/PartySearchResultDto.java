package com.back.domain.search.search.dtos;

import com.back.domain.party.party.dtos.PartyListItemDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record PartySearchResultDto(
        String query,
        List<String> extractedKeywords,
        List<PartyListItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PartySearchResultDto(String query, List<String> extractedKeywords, Page<PartyListItemDto> page) {
        this(
                query,
                extractedKeywords,
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
