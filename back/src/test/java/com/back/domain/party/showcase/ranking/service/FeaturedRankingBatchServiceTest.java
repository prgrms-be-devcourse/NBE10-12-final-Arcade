package com.back.domain.party.showcase.ranking.service;

import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.showcase.comment.entity.ShowcaseComment;
import com.back.domain.party.showcase.comment.repository.ShowcaseCommentRepository;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
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
class FeaturedRankingBatchServiceTest {

    @Autowired
    private FeaturedRankingBatchService featuredRankingBatchService;

    @Autowired
    private FeaturedRankingRepository featuredRankingRepository;

    @Autowired
    private ViewSnapshotRepository viewSnapshotRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    @Autowired
    private LikeActionRepository likeActionRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ShowcaseCommentRepository showcaseCommentRepository;

    private PartyShowcase savePublishedShowcase(String ownerEmail, String title) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();

        Party party = new Party(
                owner, "랭킹테스트팀", title, "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, "https://github.com/example/ranking",
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 1));
        party = partyRepository.save(party);

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish(title, "설명");
        return partyShowcaseRepository.save(showcase);
    }

    @Test
    @DisplayName("북마크가 좋아요보다 가중치가 높아 북마크 하나가 좋아요 하나를 이긴다")
    void bookmarkOutweighsLike() {
        PartyShowcase likedOnly = savePublishedShowcase("user1@test.com", "좋아요만");
        PartyShowcase bookmarkedOnly = savePublishedShowcase("user1@test.com", "북마크만");

        Member liker = memberRepository.findByEmail("user2@test.com").orElseThrow();
        Member bookmarker = memberRepository.findByEmail("user3@test.com").orElseThrow();
        likeActionRepository.save(new LikeAction(liker, TargetType.PARTY_SHOWCASE, likedOnly.getId()));
        bookmarkRepository.save(new Bookmark(bookmarker, TargetType.PARTY_SHOWCASE, bookmarkedOnly.getId()));

        featuredRankingBatchService.computeShowcaseRanking();

        List<FeaturedRanking> rankings = featuredRankingRepository
                .findAllByTargetTypeOrderByRankAsc(TargetType.PARTY_SHOWCASE);

        assertThat(rankings.get(0).getTargetId()).isEqualTo(bookmarkedOnly.getId());
    }

    @Test
    @DisplayName("대댓글은 댓글 점수에 반영되지 않는다")
    void repliesAreExcludedFromCommentScore() {
        PartyShowcase showcase = savePublishedShowcase("user1@test.com", "댓글 테스트");
        Member author = memberRepository.findByEmail("user2@test.com").orElseThrow();

        ShowcaseComment root = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, author, null, "원댓글"));
        showcaseCommentRepository.save(new ShowcaseComment(showcase, author, root, "대댓글"));
        showcaseCommentRepository.save(new ShowcaseComment(showcase, author, root, "대댓글2"));

        featuredRankingBatchService.computeShowcaseRanking();

        List<FeaturedRanking> rankings = featuredRankingRepository
                .findAllByTargetTypeOrderByRankAsc(TargetType.PARTY_SHOWCASE);
        FeaturedRanking ranking = rankings.stream()
                .filter(r -> r.getTargetId() == showcase.getId())
                .findFirst().orElseThrow();

        assertThat(ranking.getScore()).isCloseTo(2.0, within(0.001));
    }

    @Test
    @DisplayName("조회수는 창 시작 시점 스냅샷 대비 증가분만 점수에 반영된다")
    void viewScoreUsesWindowDelta() {
        PartyShowcase showcase = savePublishedShowcase("user1@test.com", "조회수 테스트");

        viewSnapshotRepository.save(
                new ViewSnapshot(TargetType.PARTY_SHOWCASE, showcase.getId(), 2, LocalDate.now().minusDays(29)));

        for (int i = 0; i < 5; i++) {
            partyShowcaseRepository.increaseViewCount(showcase.getId());
        }

        featuredRankingBatchService.computeShowcaseRanking();

        List<FeaturedRanking> rankings = featuredRankingRepository
                .findAllByTargetTypeOrderByRankAsc(TargetType.PARTY_SHOWCASE);
        FeaturedRanking ranking = rankings.stream()
                .filter(r -> r.getTargetId() == showcase.getId())
                .findFirst().orElseThrow();

        assertThat(ranking.getScore()).isCloseTo(0.3, within(0.001));
    }

    @Test
    @DisplayName("배치가 두 번 돌아도 오늘자 스냅샷이 중복 쌓이지 않는다")
    void rerunningBatchDoesNotDuplicateTodaySnapshot() {
        PartyShowcase showcase = savePublishedShowcase("user1@test.com", "재실행 테스트");

        featuredRankingBatchService.computeShowcaseRanking();
        featuredRankingBatchService.computeShowcaseRanking();

        List<ViewSnapshot> todaySnapshots = viewSnapshotRepository
                .findAllByTargetTypeAndTargetIdInAndSnapshotDateGreaterThanEqual(
                        TargetType.PARTY_SHOWCASE, List.of(showcase.getId()), LocalDate.now());

        assertThat(todaySnapshots).hasSize(1);
    }
}
