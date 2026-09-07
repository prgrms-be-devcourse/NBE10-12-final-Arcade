package com.back.global.initData;

import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.activity.activity.repository.ActivityLogRepository;
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
 * 한 번만 돌면 되므로 ACTIVITY_LOG 가 비어 있을 때만 동작한다.
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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (activityLogRepository.existsBy()) {
            return;
        }

        Map<Long, Map<LocalDate, Integer>> counts = new HashMap<>();

        partyRepository.findAll().forEach(party ->
                add(counts, party.getOwner().getId(), party.getCreateDate()));
        partyMemberRepository.findAll().forEach(partyMember ->
                add(counts, partyMember.getMember().getId(), partyMember.getCreateDate()));
        likeActionRepository.findAll().stream()
                .filter(like -> like.getTargetType() == TargetType.PARTY)
                .forEach(like -> add(counts, like.getMember().getId(), like.getCreateDate()));
        bookmarkRepository.findAll().stream()
                .filter(bookmark -> bookmark.getTargetType() == TargetType.PARTY)
                .forEach(bookmark -> add(counts, bookmark.getMember().getId(), bookmark.getCreateDate()));

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

    private static void add(Map<Long, Map<LocalDate, Integer>> counts, Long memberId, LocalDateTime at) {
        if (at == null) {
            return;
        }

        counts.computeIfAbsent(memberId, id -> new HashMap<>())
                .merge(at.toLocalDate(), 1, Integer::sum);
    }
}
