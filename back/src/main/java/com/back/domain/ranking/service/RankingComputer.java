package com.back.domain.ranking.service;

import com.back.domain.interaction.like.entity.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingComputer {

    private static final int TOP_N = 3;

    private final FeaturedRankingWriter featuredRankingWriter;

    public void computeAndSave(
            TargetType targetType,
            List<Long> candidateIds,
            Map<Long, Double> scoreById,
            Map<Long, LocalDateTime> latestActivityById,
            List<Long> fallbackOrderedIds
    ) {
        Comparator<Long> byScore = Comparator
                .comparingDouble((Long id) -> scoreById.getOrDefault(id, 0.0)).reversed()
                .thenComparing((Long id) -> latestActivityById.getOrDefault(id, LocalDateTime.MIN), Comparator.reverseOrder())
                .thenComparing(Comparator.<Long>naturalOrder().reversed());

        List<Long> ranked = candidateIds.stream()
                .filter(id -> scoreById.getOrDefault(id, 0.0) > 0)
                .sorted(byScore)
                .collect(Collectors.toCollection(ArrayList::new));

        if (ranked.size() < TOP_N) {
            Set<Long> already = new HashSet<>(ranked);
            for (Long id : fallbackOrderedIds) {
                if (ranked.size() >= TOP_N) {
                    break;
                }
                if (already.add(id)) {
                    ranked.add(id);
                }
            }
        }

        List<Long> top = ranked.stream().limit(TOP_N).toList();
        featuredRankingWriter.replaceTop(targetType, top, scoreById);
    }
}
