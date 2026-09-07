package com.back.domain.activity.activity.service;

import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.activity.activity.repository.ActivityLogRepository;
import com.back.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 연속 활동(스트릭)과 히트맵. 둘 다 ACTIVITY_LOG 하나에서 나온다(기획서 3.11).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLogService {

    /** 하루 경계 기준. 새벽 활동이 어제로 밀리지 않게 서버 타임존이 아니라 KST 로 고정한다 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /** 히트맵 표시 구간 - 화면이 최근 8주를 그린다 */
    public static final int HEATMAP_DAYS = 56;

    /** 스트릭 조회 구간. 배지 임계값 최대가 200일(기획서 2.9)이라 그보다 길게 읽을 이유가 없다 */
    private static final int STREAK_LOOKBACK_DAYS = 200;

    private final ActivityLogRepository activityLogRepository;

    /** 활동 하나를 오늘 자에 기록한다. 같은 날 여러 번이면 행이 늘지 않고 count 만 올라간다. */
    @Transactional
    public void record(Member actor) {
        LocalDate today = LocalDate.now(ZONE);

        activityLogRepository.findByMemberAndActivityDate(actor, today)
                .ifPresentOrElse(
                        ActivityLog::increase,
                        () -> activityLogRepository.save(new ActivityLog(actor, today)));
    }

    public Summary summary(Member actor) {
        LocalDate today = LocalDate.now(ZONE);
        Map<LocalDate, Integer> countByDate = activityLogRepository
                .findAllByMemberAndActivityDateGreaterThanEqual(actor, today.minusDays(STREAK_LOOKBACK_DAYS))
                .stream()
                .collect(Collectors.toMap(ActivityLog::getActivityDate, ActivityLog::getCount, Integer::sum));

        return new Summary(streakDays(countByDate, today), heatmap(countByDate, today));
    }

    public record Summary(int streakDays, List<Integer> heatmap) {
    }

    /**
     * 오늘부터 거슬러 올라가며 활동이 있었던 연속 일수.
     *
     * 오늘이 아직 비어 있으면 어제부터 센다 - 잔디밭과 같은 규칙이다.
     * 그렇게 하지 않으면 자정마다 모든 회원의 활동기록이 0으로 보였다가 그날 첫 활동에 되살아난다.
     */
    private static int streakDays(Map<LocalDate, Integer> countByDate, LocalDate today) {
        LocalDate cursor = countByDate.containsKey(today) ? today : today.minusDays(1);

        int streak = 0;
        while (countByDate.containsKey(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    /** 최근 8주를 오래된 날부터 늘어놓은 0~3 농도. 화면이 칸 색으로 쓴다. */
    private static List<Integer> heatmap(Map<LocalDate, Integer> countByDate, LocalDate today) {
        return IntStream.range(0, HEATMAP_DAYS)
                .mapToObj(offset -> today.minusDays(HEATMAP_DAYS - 1L - offset))
                .map(date -> level(countByDate.getOrDefault(date, 0)))
                .toList();
    }

    private static int level(int count) {
        if (count == 0) return 0;
        if (count <= 2) return 1;
        if (count <= 5) return 2;

        return 3;
    }
}
