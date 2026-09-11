package com.back.domain.party.showcase.ranking.service;

import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.party.showcase.comment.repository.ShowcaseCommentRepository;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.ranking.entity.FeaturedRanking;
import com.back.domain.party.showcase.ranking.entity.ShowcaseViewSnapshot;
import com.back.domain.party.showcase.ranking.repository.FeaturedRankingRepository;
import com.back.domain.party.showcase.ranking.repository.ShowcaseViewSnapshotRepository;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 전시실 인기 TOP3 - 매일 자정에 최근 30일 창 안의 활동으로 점수를 다시 계산해 FEATURED_RANKING에 반영한다.
// 조회수만 이벤트 로그가 없어서 스냅샷 델타로 창 안 증가분을 근사한다.
@Service
@RequiredArgsConstructor
public class FeaturedRankingBatchService {

    private static final int WINDOW_DAYS = 30;
    private static final int SNAPSHOT_RETENTION_DAYS = 31;
    private static final int TOP_N = 3;

    // 3×북마크 + 2×댓글(원댓글만) + 1×좋아요 + 0.1×조회수 - 조회수는 발생량이 훨씬 많아 자릿수를 낮춰둔다.
    private static final double BOOKMARK_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 2.0;
    private static final double LIKE_WEIGHT = 1.0;
    private static final double VIEW_WEIGHT = 0.1;

    private final PartyShowcaseRepository partyShowcaseRepository;
    private final ShowcaseViewSnapshotRepository showcaseViewSnapshotRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LikeActionRepository likeActionRepository;
    private final ShowcaseCommentRepository showcaseCommentRepository;
    private final FeaturedRankingRepository featuredRankingRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void computeShowcaseRanking() {
        LocalDate today = LocalDate.now();
        List<PartyShowcase> published = partyShowcaseRepository.findAllByPublishedTrue();

        snapshotViewCounts(published, today);
        pruneOldSnapshots(today);

        featuredRankingRepository.deleteAllByTargetType(TargetType.PARTY_SHOWCASE);
        if (published.isEmpty()) {
            return;
        }

        List<Long> showcaseIds = published.stream().map(PartyShowcase::getId).toList();
        LocalDateTime windowStart = today.minusDays(WINDOW_DAYS).atStartOfDay();

        Map<Long, Long> bookmarkCounts = bookmarkRepository
                .countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(TargetType.PARTY_SHOWCASE, showcaseIds, windowStart).stream()
                .collect(Collectors.toMap(BookmarkRepository.TargetCount::getTargetId, BookmarkRepository.TargetCount::getCount));
        Map<Long, Long> likeCounts = likeActionRepository
                .countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(TargetType.PARTY_SHOWCASE, showcaseIds, windowStart).stream()
                .collect(Collectors.toMap(LikeActionRepository.TargetCount::getTargetId, LikeActionRepository.TargetCount::getCount));
        Map<Long, Long> commentCounts = showcaseCommentRepository
                .countRootCommentsGroupedByShowcaseIdInAndCreateDateAfter(showcaseIds, windowStart).stream()
                .collect(Collectors.toMap(
                        ShowcaseCommentRepository.TargetCount::getShowcaseId,
                        ShowcaseCommentRepository.TargetCount::getCount
                ));
        Map<Long, Integer> viewBaselines = computeViewBaselines(showcaseIds, windowStart.toLocalDate());
        Map<Long, LocalDateTime> latestLikeAt = likeActionRepository
                .findLatestCreateDateGroupedByTargetTypeAndTargetIdIn(TargetType.PARTY_SHOWCASE, showcaseIds).stream()
                .collect(Collectors.toMap(
                        LikeActionRepository.TargetLatest::getTargetId,
                        LikeActionRepository.TargetLatest::getLatest
                ));

        List<Scored> scored = published.stream()
                .map(ps -> {
                    long id = ps.getId();
                    long bookmarks = bookmarkCounts.getOrDefault(id, 0L);
                    long comments = commentCounts.getOrDefault(id, 0L);
                    long likes = likeCounts.getOrDefault(id, 0L);
                    int viewDelta = Math.max(0, ps.getViewCount() - viewBaselines.getOrDefault(id, 0));
                    double score = BOOKMARK_WEIGHT * bookmarks + COMMENT_WEIGHT * comments
                            + LIKE_WEIGHT * likes + VIEW_WEIGHT * viewDelta;
                    boolean hasActivity = bookmarks > 0 || comments > 0 || likes > 0 || viewDelta > 0;
                    return new Scored(ps, score, hasActivity);
                })
                .toList();

        Comparator<Scored> byScore = Comparator
                .comparingDouble(Scored::score).reversed()
                .thenComparing((Scored s) -> latestLikeAt.getOrDefault(s.showcase().getId(), LocalDateTime.MIN), Comparator.reverseOrder())
                .thenComparing((Scored s) -> s.showcase().getId(), Comparator.reverseOrder());

        List<PartyShowcase> ranked = scored.stream()
                .filter(Scored::hasActivity)
                .sorted(byScore)
                .map(Scored::showcase)
                .collect(Collectors.toCollection(ArrayList::new));

        // 창 안 활동이 3건 미만이면 전기간 인기순(viewCount desc)으로 부족분을 채운다.
        if (ranked.size() < TOP_N) {
            Set<Long> already = ranked.stream().map(PartyShowcase::getId).collect(Collectors.toSet());
            List<PartyShowcase> fallback = partyShowcaseRepository
                    .findPublishedOrderByViewCountDesc(PageRequest.of(0, TOP_N + already.size()));
            for (PartyShowcase ps : fallback) {
                if (ranked.size() >= TOP_N) {
                    break;
                }
                if (already.add(ps.getId())) {
                    ranked.add(ps);
                }
            }
        }

        List<PartyShowcase> top = ranked.stream().limit(TOP_N).toList();
        Map<Long, Double> scoreById = scored.stream()
                .collect(Collectors.toMap(s -> s.showcase().getId(), Scored::score));

        LocalDateTime computedAt = LocalDateTime.now();
        List<FeaturedRanking> rows = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            PartyShowcase ps = top.get(i);
            rows.add(new FeaturedRanking(
                    TargetType.PARTY_SHOWCASE, ps.getId(), i + 1,
                    scoreById.getOrDefault(ps.getId(), 0.0), computedAt));
        }
        featuredRankingRepository.saveAll(rows);
    }

    private void snapshotViewCounts(List<PartyShowcase> published, LocalDate today) {
        // 재실행돼도 중복이 안 쌓이도록 오늘자 스냅샷을 먼저 지운다.
        showcaseViewSnapshotRepository.deleteBySnapshotDate(today);
        List<ShowcaseViewSnapshot> snapshots = published.stream()
                .map(ps -> new ShowcaseViewSnapshot(ps.getId(), ps.getViewCount(), today))
                .toList();
        showcaseViewSnapshotRepository.saveAll(snapshots);
    }

    private void pruneOldSnapshots(LocalDate today) {
        showcaseViewSnapshotRepository.deleteBySnapshotDateBefore(today.minusDays(SNAPSHOT_RETENTION_DAYS));
    }

    // 창 시작일 이후 가장 이른 스냅샷을 그 창의 기준값으로 삼는다. 없으면(게시 30일 미만) 0부터로 본다.
    private Map<Long, Integer> computeViewBaselines(List<Long> showcaseIds, LocalDate windowStartDate) {
        return showcaseViewSnapshotRepository
                .findAllByShowcaseIdInAndSnapshotDateGreaterThanEqual(showcaseIds, windowStartDate).stream()
                .collect(Collectors.groupingBy(
                        ShowcaseViewSnapshot::getShowcaseId,
                        Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.comparing(ShowcaseViewSnapshot::getSnapshotDate)),
                                opt -> opt.map(ShowcaseViewSnapshot::getViewCount).orElse(0)
                        )
                ));
    }

    private record Scored(PartyShowcase showcase, double score, boolean hasActivity) {
    }
}
