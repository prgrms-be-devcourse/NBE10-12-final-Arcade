package com.back.global.initData;

import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.activity.activity.repository.ActivityLogRepository;
import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * 한 번만 돌면 되므로 ACTIVITY_LOG 가 비어 있을 때만 동작한다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ActivityLogBackfill {

    @Autowired
    @Lazy
    private ActivityLogBackfill self;

    private final ActivityLogRepository activityLogRepository;
    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final LikeActionRepository likeActionRepository;
    private final BookmarkRepository bookmarkRepository;

    @Bean
    @Order(2)
    ApplicationRunner activityLogBackfillRunner() {
        return args -> self.backfill();
    }

    @Transactional
    public void backfill() {
        if (activityLogRepository.existsBy()) {
            return;
        }

        List<MemberActivity> activities = new ArrayList<>();
        partyRepository.findAll().forEach(party ->
                activities.add(new MemberActivity(party.getOwner().getId(), party.getCreateDate())));
        partyMemberRepository.findAll().forEach(partyMember ->
                activities.add(new MemberActivity(partyMember.getMember().getId(), partyMember.getCreateDate())));
        likeActionRepository.findAll().stream()
                .filter(like -> like.getTargetType() == TargetType.PARTY)
                .forEach(like -> activities.add(new MemberActivity(like.getMember().getId(), like.getCreateDate())));
        bookmarkRepository.findAll().stream()
                .filter(bookmark -> bookmark.getTargetType() == TargetType.PARTY)
                .forEach(bookmark -> activities.add(
                        new MemberActivity(bookmark.getMember().getId(), bookmark.getCreateDate())));

        Map<Long, Map<LocalDate, Integer>> countByMemberAndDate = new HashMap<>();
        activities.stream()
                .filter(activity -> activity.at() != null)
                .forEach(activity -> countByMemberAndDate
                        .computeIfAbsent(activity.memberId(), id -> new HashMap<>())
                        .merge(activity.at().atZone(ActivityLogService.ZONE).toLocalDate(), 1, Integer::sum));

        Map<Long, Member> members = memberRepository.findAllById(countByMemberAndDate.keySet()).stream()
                .collect(java.util.stream.Collectors.toMap(Member::getId, member -> member));

        List<ActivityLog> logs = countByMemberAndDate.entrySet().stream()
                .filter(entry -> members.containsKey(entry.getKey()))
                .flatMap(entry -> entry.getValue().entrySet().stream()
                        .map(day -> new ActivityLog(members.get(entry.getKey()), day.getKey(), day.getValue())))
                .toList();

        activityLogRepository.saveAll(logs);

        log.info("ACTIVITY_LOG 백필 완료 - 회원 {}명, {}행", countByMemberAndDate.size(), logs.size());
    }

    private record MemberActivity(Long memberId, LocalDateTime at) {
    }
}
