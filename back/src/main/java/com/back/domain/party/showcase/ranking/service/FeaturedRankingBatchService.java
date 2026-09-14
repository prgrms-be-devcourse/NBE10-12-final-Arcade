package com.back.domain.party.showcase.ranking.service;

import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.interaction.like.repository.TargetCount;
import com.back.domain.party.showcase.comment.repository.ShowcaseCommentRepository;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import com.back.domain.ranking.service.FeaturedRankingWriter;
import com.back.domain.ranking.service.RankingComputer;
import com.back.domain.ranking.service.ViewSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeaturedRankingBatchService {

    private static final int WINDOW_DAYS = 30;
    private static final int TOP_N = 3;

    private static final double BOOKMARK_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 2.0;
    private static final double LIKE_WEIGHT = 1.0;
    private static final double VIEW_WEIGHT = 0.1;

    private final PartyShowcaseRepository partyShowcaseRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LikeActionRepository likeActionRepository;
    private final ShowcaseCommentRepository showcaseCommentRepository;
    private final ViewSnapshotService viewSnapshotService;
    private final FeaturedRankingWriter featuredRankingWriter;
    private final RankingComputer rankingComputer;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void computeShowcaseRanking() {
        LocalDate today = LocalDate.now();
        List<PartyShowcase> published = partyShowcaseRepository.findAllByPublishedTrue();

        Map<Long, Integer> currentViewCounts = published.stream()
                .collect(Collectors.toMap(PartyShowcase::getId, PartyShowcase::getViewCount));
        viewSnapshotService.snapshotViewCounts(TargetType.PARTY_SHOWCASE, currentViewCounts, today);

        if (published.isEmpty()) {
            featuredRankingWriter.replaceTop(TargetType.PARTY_SHOWCASE, List.of(), Map.of());
            return;
        }

        List<Long> showcaseIds = published.stream().map(PartyShowcase::getId).toList();
        LocalDateTime windowStart = today.minusDays(WINDOW_DAYS).atStartOfDay();

        Map<Long, Long> bookmarkCounts = bookmarkRepository
                .countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(TargetType.PARTY_SHOWCASE, showcaseIds, windowStart).stream()
                .collect(Collectors.toMap(TargetCount::getTargetId, TargetCount::getCount));
        Map<Long, Long> likeCounts = likeActionRepository
                .countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(TargetType.PARTY_SHOWCASE, showcaseIds, windowStart).stream()
                .collect(Collectors.toMap(TargetCount::getTargetId, TargetCount::getCount));
        Map<Long, Long> commentCounts = showcaseCommentRepository
                .countRootCommentsGroupedByShowcaseIdInAndCreateDateAfter(showcaseIds, windowStart).stream()
                .collect(Collectors.toMap(
                        ShowcaseCommentRepository.TargetCount::getShowcaseId,
                        ShowcaseCommentRepository.TargetCount::getCount
                ));
        Map<Long, Integer> viewBaselines = viewSnapshotService
                .computeViewBaselines(TargetType.PARTY_SHOWCASE, showcaseIds, windowStart.toLocalDate());
        Map<Long, LocalDateTime> latestLikeAt = likeActionRepository
                .findLatestCreateDateGroupedByTargetTypeAndTargetIdIn(TargetType.PARTY_SHOWCASE, showcaseIds).stream()
                .collect(Collectors.toMap(
                        LikeActionRepository.TargetLatest::getTargetId,
                        LikeActionRepository.TargetLatest::getLatest
                ));

        Map<Long, Double> scoreById = published.stream()
                .collect(Collectors.toMap(PartyShowcase::getId, ps -> {
                    long id = ps.getId();
                    long bookmarks = bookmarkCounts.getOrDefault(id, 0L);
                    long comments = commentCounts.getOrDefault(id, 0L);
                    long likes = likeCounts.getOrDefault(id, 0L);
                    int viewDelta = Math.max(0, ps.getViewCount() - viewBaselines.getOrDefault(id, 0));
                    return BOOKMARK_WEIGHT * bookmarks + COMMENT_WEIGHT * comments
                            + LIKE_WEIGHT * likes + VIEW_WEIGHT * viewDelta;
                }));

        List<Long> fallbackOrderedIds = partyShowcaseRepository
                .findPublishedOrderByViewCountDesc(PageRequest.of(0, TOP_N * 2)).stream()
                .map(PartyShowcase::getId)
                .toList();

        rankingComputer.computeAndSave(TargetType.PARTY_SHOWCASE, showcaseIds, scoreById, latestLikeAt, fallbackOrderedIds);
    }
}
