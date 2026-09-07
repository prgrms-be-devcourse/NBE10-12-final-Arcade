package com.back.global.initData;

import com.back.RedisTestContainerConfig;
import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.activity.activity.repository.ActivityLogRepository;
import com.back.domain.activity.activity.service.ActivityLogService;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@Import(RedisTestContainerConfig.class)
class ActivityLogBackfillTest {

    @Autowired
    private ActivityLogBackfill activityLogBackfill;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private LikeActionRepository likeActionRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private EntityManager entityManager;

    private Member owner;

    @BeforeEach
    void setUp() {
        owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        // 애플리케이션 기동 시 이미 한 번 돌았을 수 있어, 백필 전 상태로 되돌린다
        activityLogRepository.deleteAll();
        activityLogRepository.flush();
    }

    /** createDate 는 auditing 이 채우므로, 과거 시각을 만들려면 저장 후 직접 밀어야 한다. */
    private void backdate(String table, long id, LocalDateTime at) {
        entityManager.createNativeQuery(
                        "update " + table + " set create_date = :at where id = :id")
                .setParameter("at", at)
                .setParameter("id", id)
                .executeUpdate();
    }

    private Party saveParty(int daysAgo) {
        Party party = new Party(
                owner, "백필 테스트 파티", "제목", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null, 1, LocalDateTime.now().plusDays(7));
        party.addPosition(new Position(PositionType.BACK, 2));
        partyRepository.saveAndFlush(party);

        backdate("party", party.getId(), LocalDateTime.now().minusDays(daysAgo));

        return party;
    }

    @Test
    @DisplayName("백필: 과거 파티 생성·좋아요·북마크를 날짜별로 묶어 채운다")
    void backfillsPastActivity() {
        Party party = saveParty(3);

        LikeAction like = likeActionRepository.saveAndFlush(
                new LikeAction(owner, TargetType.PARTY, party.getId()));
        backdate("like_action", like.getId(), LocalDateTime.now().minusDays(3));

        Bookmark bookmark = bookmarkRepository.saveAndFlush(
                new Bookmark(owner, TargetType.PARTY, party.getId()));
        backdate("bookmark", bookmark.getId(), LocalDateTime.now().minusDays(1));

        entityManager.clear();

        activityLogBackfill.run(null);

        LocalDate today = LocalDate.now(ActivityLogService.ZONE);

        // 3일 전에 파티 생성 + 좋아요 = 한 행에 count 2
        assertThat(activityLogRepository.findByMemberAndActivityDate(owner, today.minusDays(3)))
                .get().extracting(ActivityLog::getCount).isEqualTo(2);
        assertThat(activityLogRepository.findByMemberAndActivityDate(owner, today.minusDays(1)))
                .get().extracting(ActivityLog::getCount).isEqualTo(1);
        assertThat(activityLogRepository.findByMemberAndActivityDate(owner, today.minusDays(2)))
                .isEmpty();
    }

    @Test
    @DisplayName("백필 안전핀: 스트릭 조회 창(200일)보다 오래된 활동은 읽지 않는다")
    void skipsActivityOlderThanLookbackWindow() {
        saveParty(500);

        entityManager.clear();

        activityLogBackfill.run(null);

        assertThat(activityLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("백필 안전핀: 이미 채워져 있으면 다시 읽지 않는다")
    void doesNotRunTwice() {
        LocalDate today = LocalDate.now(ActivityLogService.ZONE);
        activityLogRepository.saveAndFlush(new ActivityLog(owner, today.minusDays(10), 7));

        saveParty(3);
        entityManager.clear();

        activityLogBackfill.run(null);

        // 기존 한 행 그대로. 파티 생성분이 새로 들어오지 않았다.
        assertThat(activityLogRepository.findAll()).hasSize(1);
        assertThat(activityLogRepository.findByMemberAndActivityDate(owner, today.minusDays(10)))
                .get().extracting(ActivityLog::getCount).isEqualTo(7);
    }
}
