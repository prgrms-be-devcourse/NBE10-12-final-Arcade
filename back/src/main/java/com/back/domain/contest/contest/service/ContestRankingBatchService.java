package com.back.domain.contest.contest.service;

import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.interaction.like.repository.TargetCount;
import com.back.domain.party.party.repository.PartyContestLookupPort;
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
public class ContestRankingBatchService {

    private static final int WINDOW_DAYS = 30;
    private static final int TOP_N = 3;

    private static final double PARTICIPATING_PARTY_WEIGHT = 4.0;
    private static final double BOOKMARK_WEIGHT = 3.0;
    private static final double LIKE_WEIGHT = 1.0;
    private static final double VIEW_WEIGHT = 0.1;

    private final ContestPostRepository contestPostRepository;
    private final PartyContestLookupPort partyContestLookupPort;
    private final BookmarkRepository bookmarkRepository;
    private final LikeActionRepository likeActionRepository;
    private final ViewSnapshotService viewSnapshotService;
    private final FeaturedRankingWriter featuredRankingWriter;
    private final RankingComputer rankingComputer;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void computeContestRanking() {
        LocalDate today = LocalDate.now();
        List<ContestPost> posts = contestPostRepository.findAllWithContest();

        Map<Long, Integer> currentViewCounts = posts.stream()
                .collect(Collectors.toMap(cp -> cp.getContest().getId(), ContestPost::getViewCount));
        viewSnapshotService.snapshotViewCounts(TargetType.CONTEST, currentViewCounts, today);

        if (posts.isEmpty()) {
            featuredRankingWriter.replaceTop(TargetType.CONTEST, List.of(), Map.of());
            return;
        }

        List<Long> contestIds = posts.stream().map(cp -> cp.getContest().getId()).toList();
        LocalDateTime windowStart = today.minusDays(WINDOW_DAYS).atStartOfDay();

        Map<Long, Long> participatingPartyCounts = partyContestLookupPort
                .countGroupedByTargetContestIdInAndCreateDateAfter(contestIds, windowStart).stream()
                .collect(Collectors.toMap(PartyContestLookupPort.TeamCount::getContestId, PartyContestLookupPort.TeamCount::getCount));
        Map<Long, Long> bookmarkCounts = bookmarkRepository
                .countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(TargetType.CONTEST, contestIds, windowStart).stream()
                .collect(Collectors.toMap(TargetCount::getTargetId, TargetCount::getCount));
        Map<Long, Long> likeCounts = likeActionRepository
                .countGroupedByTargetTypeAndTargetIdInAndCreateDateAfter(TargetType.CONTEST, contestIds, windowStart).stream()
                .collect(Collectors.toMap(TargetCount::getTargetId, TargetCount::getCount));
        Map<Long, Integer> viewBaselines = viewSnapshotService
                .computeViewBaselines(TargetType.CONTEST, contestIds, windowStart.toLocalDate());
        Map<Long, LocalDateTime> latestLikeAt = likeActionRepository
                .findLatestCreateDateGroupedByTargetTypeAndTargetIdIn(TargetType.CONTEST, contestIds).stream()
                .collect(Collectors.toMap(
                        LikeActionRepository.TargetLatest::getTargetId,
                        LikeActionRepository.TargetLatest::getLatest
                ));

        Map<Long, Double> scoreById = posts.stream()
                .collect(Collectors.toMap(cp -> cp.getContest().getId(), cp -> {
                    long id = cp.getContest().getId();
                    long parties = participatingPartyCounts.getOrDefault(id, 0L);
                    long bookmarks = bookmarkCounts.getOrDefault(id, 0L);
                    long likes = likeCounts.getOrDefault(id, 0L);
                    int viewDelta = Math.max(0, cp.getViewCount() - viewBaselines.getOrDefault(id, 0));
                    return PARTICIPATING_PARTY_WEIGHT * parties + BOOKMARK_WEIGHT * bookmarks
                            + LIKE_WEIGHT * likes + VIEW_WEIGHT * viewDelta;
                }));

        List<Long> fallbackOrderedIds = contestPostRepository
                .searchOrderByPopular(null, null, PageRequest.of(0, TOP_N * 2))
                .getContent().stream()
                .map(cp -> cp.getContest().getId())
                .toList();

        rankingComputer.computeAndSave(TargetType.CONTEST, contestIds, scoreById, latestLikeAt, fallbackOrderedIds);
    }
}
