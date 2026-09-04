package com.back.domain.search.search.service.keyword;

import com.back.domain.search.search.entity.keyword.KeywordRelatedTerm;
import com.back.domain.search.search.repository.keyword.KeywordRelatedTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatedTermExpansionService implements RelatedTermExpansionPort {

    private final KeywordRelatedTermRepository keywordRelatedTermRepository;

    @Override
    public List<String> expand(List<String> keywords) {
        Set<String> expanded = new LinkedHashSet<>(keywords);

        keywordRelatedTermRepository.findByTermIn(keywords)
                .forEach(relation -> expanded.add(relation.getRelatedTerm()));

        return List.copyOf(expanded);
    }
}
