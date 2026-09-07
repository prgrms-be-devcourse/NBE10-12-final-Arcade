package com.back.global.initData;

import com.back.domain.activity.activity.dtos.MemberActivityAt;
import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.activity.activity.repository.ActivityLogRepository;
import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACTIVITY_LOG 를 과거 활동으로 한 번 채운다.
 *
 * 이 테이블은 나중에 생겨서 그냥 두면 오늘부터만 쌓이고, 기존 회원 전원의 스트릭·히트맵이 0에서 시작한다.
 * 파티 생성·지원·파티 좋아요·북마크는 각 테이블의 createDate 에 시각이 남아 있어 소급해서 셀 수 있다.
 *
 * 승인/거절 처리·모집 마감·완료 판정은 그 행동이 일어난 시각을 따로 남기지 않아(modifyDate 는 이후 수정에도
 * 갱신된다) 백필 대상에서 뺐다. 앞으로 발생하는 건 정상적으로 기록된다.
 *
 * 두 겹의 안전핀을 둔다.
 * 1. ACTIVITY_LOG 가 비어 있을 때만 돈다 - 한 번 채우고 나면 다시 읽지 않는다.
 * 2. 최근 BACKFILL_DAYS 안의 행만 읽는다 - 그보다 오래된 활동은 스트릭 조회 창(200일)에도,
 *    히트맵(8주)에도 안 잡혀서 채워봐야 화면에 나오지 않는다. 전체 테이블을 메모리에 올리지 않는
 *    가장 확실한 경계가 '어차피 쓰이지 않는 구간을 안 읽는 것'이다.
 *
 * 읽을 때도 엔티티가 아니라 (회원 id, 시각) 투영만 가져온다.
 *
 * ponytail: 200일 윈도우 + 투영 조회로 유계. 이 구간 활동량이 메모리에 부담될 만큼 커지면
 * INSERT INTO activity_log ... SELECT ... GROUP BY 네이티브 한 방으로 바꿀 것.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ActivityLogBackfill implements ApplicationRunner {

    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final LikeActionRepository likeActionRepository;
    private final BookmarkRepository bookmarkRepository;

    /** 스트릭 조회 창과 같은 길이. 이보다 오래된 활동은 어디에도 표시되지 않는다. */
    private static final int BACKFILL_DAYS = 200;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (activityLogRepository.existsBy()) {
            return;
        }

        LocalDateTime from = LocalDate.now(ActivityLogService.ZONE)
                .minusDays(BACKFILL_DAYS)
                .atStartOfDay();

        Map<Long, Map<LocalDate, Integer>> counts = new HashMap<>();

        add(counts, partyRepository.findActivityAtSince(from));
        add(counts, partyMemberRepository.findActivityAtSince(from));
        add(counts, likeActionRepository.findActivityAtSince(TargetType.PARTY, from));
        add(counts, bookmarkRepository.findActivityAtSince(TargetType.PARTY, from));

        // 회원은 FK 만 채우면 되므로 실제로 읽지 않고 프록시를 쓴다.
        // id 를 파티·지원·좋아요·북마크 행에서 뽑았으니 가리키는 회원이 없을 수 없다.
        List<ActivityLog> logs = counts.entrySet().stream()
                .flatMap(member -> member.getValue().entrySet().stream()
                        .map(day -> new ActivityLog(
                                memberRepository.getReferenceById(member.getKey()),
                                day.getKey(),
                                day.getValue())))
                .toList();

        activityLogRepository.saveAll(logs);

        log.info("ACTIVITY_LOG 백필 완료 - 회원 {}명, {}행", counts.size(), logs.size());
    }

    private static void add(Map<Long, Map<LocalDate, Integer>> counts, List<MemberActivityAt> activities) {
        activities.forEach(activity -> counts
                .computeIfAbsent(activity.memberId(), id -> new HashMap<>())
                .merge(activity.at().toLocalDate(), 1, Integer::sum));
    }
}
