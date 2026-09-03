package com.back.domain.search.search.service;

import com.back.domain.search.search.entity.KeywordRelatedTerm;
import com.back.domain.search.search.repository.KeywordRelatedTermRepository;
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

        for (String keyword : keywords) {
            keywordRelatedTermRepository.findByTerm(keyword)
                    .forEach(relation -> expanded.add(relation.getRelatedTerm()));
        }

        return List.copyOf(expanded);
    }
}
