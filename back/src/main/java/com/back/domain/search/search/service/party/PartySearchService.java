package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.dtos.PartySearchResultDto;
import com.back.domain.search.search.service.SearchLogService;
import com.back.domain.search.search.service.keyword.KeywordExtractionPort;
import com.back.domain.search.search.service.keyword.KeywordNormalizationPort;
import com.back.domain.search.search.service.keyword.RelatedTermExpansionPort;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartySearchService {

    private final KeywordExtractionPort keywordExtractionPort;
    private final KeywordNormalizationPort keywordNormalizationPort;
    private final RelatedTermExpansionPort relatedTermExpansionPort;
    private final PartyMatchQueryPort partyMatchQueryPort;
    private final PartyRepository partyRepository;
    private final SearchLogService searchLogService;

    public PartySearchResultDto search(Member actor, String query, Pageable pageable) {
        List<String> extracted = keywordExtractionPort.extract(query);
        if (extracted.isEmpty()) {
            throw new ServiceException("400-4", "검색어가 너무 짧습니다.");
        }
        List<String> normalized = keywordNormalizationPort.normalize(extracted);
        List<String> expanded = relatedTermExpansionPort.expand(normalized);

        try {
            searchLogService.log(actor, query);
        } catch (Exception e) {
            log.warn("검색 기록 저장에 실패했습니다.", e);
        }

        Page<Long> matchedIds = partyMatchQueryPort.findMatchingPartyIds(expanded, pageable);
        List<Long> ids = matchedIds.getContent();
        Map<Long, Party> partyById = partyRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Party::getId, Function.identity()));
        List<Party> parties = ids.stream()
                .map(partyById::get)
                .filter(Objects::nonNull)
                .toList();
        long missing = ids.size() - parties.size();
        Page<Party> matched = new PageImpl<>(parties, pageable, matchedIds.getTotalElements() - missing);

        Page<PartyListItemDto> dtoPage = matched.map(PartyListItemDto::new);
        return new PartySearchResultDto(query, expanded, dtoPage);
    }
}
