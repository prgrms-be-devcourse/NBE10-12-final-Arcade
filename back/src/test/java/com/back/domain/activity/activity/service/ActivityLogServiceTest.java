package com.back.domain.activity.activity.service;

import com.back.RedisTestContainerConfig;
import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.activity.activity.repository.ActivityLogRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@Import(RedisTestContainerConfig.class)
class ActivityLogServiceTest {

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member actor() {
        return memberRepository.findByEmail("user1@test.com").orElseThrow();
    }

    private LocalDate today() {
        return LocalDate.now(ActivityLogService.ZONE);
    }

    private void log(Member member, int daysAgo, int count) {
        activityLogRepository.save(new ActivityLog(member, today().minusDays(daysAgo), count));
    }

    @Test
    @DisplayName("기록: 같은 날 여러 번이면 행이 늘지 않고 count 만 올라간다")
    void recordSameDayIncreasesCount() {
        Member actor = actor();

        activityLogService.record(actor);
        activityLogService.record(actor);
        activityLogService.record(actor);

        var logs = activityLogRepository.findAllByMemberAndActivityDateGreaterThanEqual(
                actor, today().minusDays(1));

        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getCount()).isEqualTo(3);
        assertThat(activityLogService.summary(actor).streakDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("스트릭: 오늘부터 이어진 날만 센다 - 끊긴 앞쪽은 세지 않는다")
    void streakCountsOnlyConsecutiveDays() {
        Member actor = actor();

        log(actor, 0, 1);
        log(actor, 1, 2);
        log(actor, 2, 1);
        // 3일 전은 비어 있다 - 여기서 끊긴다
        log(actor, 4, 5);
        log(actor, 5, 1);

        assertThat(activityLogService.summary(actor).streakDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("스트릭: 오늘이 비어도 어제까지의 연속을 유지한다 - 자정에 0으로 떨어지지 않게")
    void streakSurvivesEmptyToday() {
        Member actor = actor();

        log(actor, 1, 1);
        log(actor, 2, 1);

        assertThat(activityLogService.summary(actor).streakDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("스트릭: 어제도 오늘도 비어 있으면 0이다")
    void streakBreaksWhenYesterdayAlsoEmpty() {
        Member actor = actor();

        log(actor, 2, 9);
        log(actor, 3, 9);

        assertThat(activityLogService.summary(actor).streakDays()).isZero();
    }

    @Test
    @DisplayName("스트릭: 기록이 하나도 없으면 0이고 히트맵은 전부 0이다")
    void streakIsZeroWithoutAnyActivity() {
        ActivityLogService.Summary summary = activityLogService.summary(actor());

        assertThat(summary.streakDays()).isZero();
        assertThat(summary.heatmap()).hasSize(ActivityLogService.HEATMAP_DAYS).containsOnly(0);
    }

    @Test
    @DisplayName("히트맵: 최근 8주를 오래된 날부터 늘어놓고 count 를 0~3 농도로 바꾼다")
    void heatmapMapsCountToLevel() {
        Member actor = actor();

        log(actor, 0, 7);   // 6 이상 -> 3
        log(actor, 1, 4);   // 3~5   -> 2
        log(actor, 2, 1);   // 1~2   -> 1
        // 3일 전은 없음      // 0     -> 0

        var heatmap = activityLogService.summary(actor).heatmap();
        int last = ActivityLogService.HEATMAP_DAYS - 1;

        assertThat(heatmap).hasSize(ActivityLogService.HEATMAP_DAYS);
        assertThat(heatmap.get(last)).isEqualTo(3);
        assertThat(heatmap.get(last - 1)).isEqualTo(2);
        assertThat(heatmap.get(last - 2)).isEqualTo(1);
        assertThat(heatmap.get(last - 3)).isZero();
    }

    @Test
    @DisplayName("스트릭·히트맵: 남의 활동은 섞이지 않는다")
    void summaryIsScopedToOneMember() {
        Member actor = actor();
        Member stranger = memberRepository.findByEmail("user2@test.com").orElseThrow();

        log(stranger, 0, 3);
        log(stranger, 1, 3);

        ActivityLogService.Summary summary = activityLogService.summary(actor);

        assertThat(summary.streakDays()).isZero();
        assertThat(summary.heatmap()).containsOnly(0);
    }

    @Test
    @DisplayName("히트맵: 8주보다 오래된 기록은 히트맵에 안 들어가지만 스트릭 계산에는 쓰인다")
    void heatmapWindowIsShorterThanStreakWindow() {
        Member actor = actor();

        for (int daysAgo = 0; daysAgo <= 60; daysAgo++) {
            log(actor, daysAgo, 1);
        }

        ActivityLogService.Summary summary = activityLogService.summary(actor);

        assertThat(summary.streakDays()).isEqualTo(61);
        assertThat(summary.heatmap()).hasSize(ActivityLogService.HEATMAP_DAYS).containsOnly(1);
    }
}
