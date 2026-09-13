package com.back.domain.ranking.service;

import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.ranking.entity.ViewSnapshot;
import com.back.domain.ranking.repository.ViewSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViewSnapshotService {

    private static final int RETENTION_DAYS = 31;

    private final ViewSnapshotRepository viewSnapshotRepository;

    public void snapshotViewCounts(TargetType targetType, Map<Long, Integer> viewCountsByTargetId, LocalDate today) {
        viewSnapshotRepository.deleteByTargetTypeAndSnapshotDate(targetType, today);
        viewSnapshotRepository.saveAll(
                viewCountsByTargetId.entrySet().stream()
                        .map(e -> new ViewSnapshot(targetType, e.getKey(), e.getValue(), today))
                        .toList()
        );
    }

    public void pruneOldSnapshots(LocalDate today) {
        viewSnapshotRepository.deleteBySnapshotDateBefore(today.minusDays(RETENTION_DAYS));
    }

    public Map<Long, Integer> computeViewBaselines(TargetType targetType, Collection<Long> targetIds, LocalDate windowStartDate) {
        return viewSnapshotRepository
                .findAllByTargetTypeAndTargetIdInAndSnapshotDateGreaterThanEqual(targetType, targetIds, windowStartDate).stream()
                .collect(Collectors.groupingBy(
                        ViewSnapshot::getTargetId,
                        Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.comparing(ViewSnapshot::getSnapshotDate)),
                                opt -> opt.map(ViewSnapshot::getViewCount).orElse(0)
                        )
                ));
    }
}
