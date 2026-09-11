package com.back.domain.ranking.service;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.ranking.entity.FeaturedRanking;
import com.back.domain.ranking.repository.FeaturedRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeaturedRankingWriter {

    private final FeaturedRankingRepository featuredRankingRepository;

    public void replaceTop(TargetType targetType, List<Long> orderedTargetIds, Map<Long, Double> scoreById) {
        featuredRankingRepository.deleteAllByTargetType(targetType);

        LocalDateTime computedAt = LocalDateTime.now();
        List<FeaturedRanking> rows = new ArrayList<>();
        for (int i = 0; i < orderedTargetIds.size(); i++) {
            long targetId = orderedTargetIds.get(i);
            rows.add(new FeaturedRanking(
                    targetType, targetId, i + 1, scoreById.getOrDefault(targetId, 0.0), computedAt));
        }
        featuredRankingRepository.saveAll(rows);
    }
}
