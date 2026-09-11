package com.back.domain.contest.contest.service;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.ranking.entity.FeaturedRanking;
import com.back.domain.ranking.entity.ViewSnapshot;
import com.back.domain.ranking.repository.FeaturedRankingRepository;
import com.back.domain.ranking.repository.ViewSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ContestRankingBatchServiceTest {

    @Autowired
    private ContestRankingBatchService contestRankingBatchService;

    @Autowired
    private ContestService contestService;

    @Autowired
    private FeaturedRankingRepository featuredRankingRepository;

    @Autowired
    private ViewSnapshotRepository viewSnapshotRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ContestRepository contestRepository;

    private long writeContest(String title) {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();
        ContestResponseDto contest = contestService.write(
                admin, title, ContestFormat.HACKATHON, ContestTag.AI,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                "설명", "https://example.com/contest", null
        );
        return contest.id();
    }

    @Test
    @DisplayName("참여 파티 수가 북마크보다 가중치가 높아 참여 파티 하나가 북마크 하나를 이긴다")
    void participatingPartyOutweighsBookmark() {
        long bookmarkOnlyId = writeContest("북마크만");
        long partyOnlyId = writeContest("참여파티만");

        Member bookmarker = memberRepository.findByEmail("user2@test.com").orElseThrow();
        bookmarkRepository.save(new Bookmark(bookmarker, TargetType.CONTEST, bookmarkOnlyId));

        Member owner = memberRepository.findByEmail("admin").orElseThrow();
        Contest targetContest = contestRepository.findById(partyOnlyId).orElseThrow();
        Party party = new Party(
                owner, "참여파티", "파티 제목", "설명",
                targetContest, null, null, TopicType.CONTEST, PartyTag.WEB, null,
                LocalDateTime.now().plusDays(7)
        );
        partyRepository.save(party);

        contestRankingBatchService.computeContestRanking();

        List<FeaturedRanking> rankings = featuredRankingRepository
                .findAllByTargetTypeOrderByRankAsc(TargetType.CONTEST);

        assertThat(rankings.get(0).getTargetId()).isEqualTo(partyOnlyId);
    }

    @Test
    @DisplayName("대회 조회수는 창 시작 시점 스냅샷 대비 증가분만 점수에 반영된다")
    void viewScoreUsesWindowDelta() {
        long contestId = writeContest("조회수 테스트");

        viewSnapshotRepository.save(
                new ViewSnapshot(TargetType.CONTEST, contestId, 2, LocalDate.now().minusDays(29)));

        for (int i = 0; i < 5; i++) {
            contestService.getDetail(contestId, true);
        }

        contestRankingBatchService.computeContestRanking();

        List<FeaturedRanking> rankings = featuredRankingRepository
                .findAllByTargetTypeOrderByRankAsc(TargetType.CONTEST);
        FeaturedRanking ranking = rankings.stream()
                .filter(r -> r.getTargetId() == contestId)
                .findFirst().orElseThrow();

        assertThat(ranking.getScore()).isCloseTo(0.3, within(0.001));
    }
}
