package com.back.domain.interaction.like.service;

import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.interaction.like.dtos.LikeDto;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService implements LikeInteractionPort {

    private final LikeActionRepository likeActionRepository;
    private final ActivityLogService activityLogService;
    private final PartyRepository partyRepository;
    private final ContestPostRepository contestPostRepository;
    private final GoalRepository goalRepository;
    private final PartyShowcaseRepository partyShowcaseRepository;

    public boolean partyExists(long partyId) {
        return partyRepository.existsById(partyId);
    }

    public boolean contestPostExists(long contestId) {
        return contestPostRepository.existsByContestId(contestId);
    }

    public boolean goalExists(long goalId) {
        return goalRepository.findById(goalId)
                .map(Goal::isExhibited)
                .orElse(false);
    }

    public boolean isLiked(Member member, TargetType targetType, long targetId) {
        return likeActionRepository.existsByMemberAndTargetTypeAndTargetId(member, targetType, targetId);
    }

    // goalId 기준 이미 좋아요했는지 확인 - PROJECT는 실제 좋아요가 PARTY 타깃으로 저장되므로 단순히 TargetType.GOAL로 체크하면 항상 false가 나와 중복 좋아요를 못 막는다
    // resolveGoalLikeTarget()으로 실제 저장 위치를 먼저 알아낸 뒤 그 타깃으로 확인해야 한다
    public boolean isGoalLiked(long goalId, Member member) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        LikeTarget target = resolveGoalLikeTarget(goal);
        return isLiked(member, target.targetType(), target.targetId());
    }

    @Transactional
    public LikeDto likeParty(long partyId, Member member) {
        likeActionRepository.save(new LikeAction(member, TargetType.PARTY, partyId));
        partyRepository.increaseLikeCount(partyId);
        activityLogService.record(member);

        int updatedLikeCount = partyRepository.findById(partyId).orElseThrow().getLikeCount();
        return new LikeDto(TargetType.PARTY, partyId, true, updatedLikeCount);
    }

    @Transactional
    public void unlikeParty(long partyId, Member member) {
        likeActionRepository.deleteByMemberAndTargetTypeAndTargetId(member, TargetType.PARTY, partyId);
        partyRepository.decreaseLikeCount(partyId);
    }

    @Transactional
    public LikeDto likeContest(long contestId, Member member) {
        likeActionRepository.save(new LikeAction(member, TargetType.CONTEST, contestId));
        contestPostRepository.increaseLikeCount(contestId);

        int updatedLikeCount = findContestPostOrThrow(contestId).getLikeCount();
        return new LikeDto(TargetType.CONTEST, contestId, true, updatedLikeCount);
    }

    @Transactional
    public void unlikeContest(long contestId, Member member) {
        likeActionRepository.deleteByMemberAndTargetTypeAndTargetId(member, TargetType.CONTEST, contestId);
        contestPostRepository.decreaseLikeCount(contestId);
    }

    // 전시 성취의 좋아요, 북마크는 GOAL 자신이 아니라 sourcePartyId가 가리키는 파티의 전시 게시글을 대상으로 건다
    // 같은 프로젝트의 참여자 전원의 카드가 동일한 전시글 1건을 가리키므로 좋아요·북마크가 자연스럽게 하나로 합산된다.
    // 모집글 좋아요와 전시글 좋아요는 시점도 응답자 의도도 다른 별개 지표라, 모집글 자신이 아니라 그 파티의 전시글로 라우팅한다
    // PARTY로 라우팅하면 모집 단계 좋아요랑 뒤섞여 버린다
    @Transactional
    public LikeDto likeGoal(long goalId, Member member) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        LikeTarget target = resolveGoalLikeTarget(goal);

        likeActionRepository.save(new LikeAction(member, target.targetType(), target.targetId()));

        int updatedLikeCount;
        if (target.targetType() == TargetType.PARTY_SHOWCASE) {
            partyShowcaseRepository.increaseLikeCount(target.targetId());
            updatedLikeCount = partyShowcaseRepository.findById(target.targetId()).orElseThrow().getLikeCount();
        } else {
            goalRepository.increaseLikeCount(target.targetId());
            updatedLikeCount = goalRepository.findById(target.targetId()).orElseThrow().getLikeCount();
        }

        // 응답은 항상 요청받은 goalId 기준으로 내려준다 - 내부적으로 PARTY로 라우팅됐다는 사실을
        // 클라이언트가 알 필요는 없다(API 계약을 goalId로 통일).
        return new LikeDto(TargetType.GOAL, goalId, true, updatedLikeCount);
    }

    @Transactional
    public void unlikeGoal(long goalId, Member member) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        LikeTarget target = resolveGoalLikeTarget(goal);

        likeActionRepository.deleteByMemberAndTargetTypeAndTargetId(member, target.targetType(), target.targetId());

        if (target.targetType() == TargetType.PARTY_SHOWCASE) {
            partyShowcaseRepository.decreaseLikeCount(target.targetId());
        } else {
            goalRepository.decreaseLikeCount(target.targetId());
        }
    }

    private record LikeTarget(TargetType targetType, long targetId) {
    }

    private LikeTarget resolveGoalLikeTarget(Goal goal) {
        if (goal instanceof Project project) {
            return new LikeTarget(TargetType.PARTY_SHOWCASE, project.getPartyShowcase().getId());
        }
        return new LikeTarget(TargetType.GOAL, goal.getId());
    }

    @Override
    @Transactional
    public void deleteAllLikesForTarget(TargetType targetType, long targetId) {
        likeActionRepository.deleteAllByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public Set<Long> findLikedTargetIds(Member member, TargetType targetType, Collection<Long> targetIds) {
        if (member == null || targetIds.isEmpty()) {
            return Set.of();
        }

        if (targetType != TargetType.GOAL) {
            return new HashSet<>(likeActionRepository.findTargetIdsByMemberAndTargetTypeAndTargetIdIn(member, targetType, targetIds));
        }

        List<Goal> goals = goalRepository.findAllById(targetIds);

        Map<Long, Long> projectGoalIdToShowcaseId = new HashMap<>();
        Set<Long> plainGoalIds = new HashSet<>();

        for (Goal goal : goals) {
            if (goal instanceof Project project) {
                projectGoalIdToShowcaseId.put(goal.getId(), project.getPartyShowcase().getId());
            } else {
                plainGoalIds.add(goal.getId());
            }
        }

        Set<Long> likedGoalIds = new HashSet<>();

        if (!plainGoalIds.isEmpty()) {
            likedGoalIds.addAll(likeActionRepository.findTargetIdsByMemberAndTargetTypeAndTargetIdIn(
                    member, TargetType.GOAL, plainGoalIds
            ));
        }

        if (!projectGoalIdToShowcaseId.isEmpty()) {
            Set<Long> showcaseIds = new HashSet<>(projectGoalIdToShowcaseId.values());
            Set<Long> likedShowcaseIds = new HashSet<>(likeActionRepository.findTargetIdsByMemberAndTargetTypeAndTargetIdIn(
                    member, TargetType.PARTY_SHOWCASE, showcaseIds
            ));

            // 좋아요된 showcaseId를 다시 원래 요청받은 goalId로 되돌린다
            for (Map.Entry<Long, Long> entry : projectGoalIdToShowcaseId.entrySet()) {
                if (likedShowcaseIds.contains(entry.getValue())) {
                    likedGoalIds.add(entry.getKey());
                }
            }
        }

        return likedGoalIds;
    }

    private ContestPost findContestPostOrThrow(long contestId) {
        return contestPostRepository.findByContestId(contestId).orElseThrow();
    }
}
