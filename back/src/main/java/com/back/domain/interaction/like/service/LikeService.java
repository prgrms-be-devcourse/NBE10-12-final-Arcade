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

    // PROJECT는 파티 확정 시 참여자 수만큼 Goal이 따로 생기므로 LikeAction
    // 행을 Goal 자신으로 저장하면 파티 페이지에서 직접 누른 좋아요와 성취 카드에서 누른 좋아요가
    // 서로 다른 행으로 취급돼 같은 사람이 두 경로로 각각 좋아요를 눌러 카운트가 중복 반영될 수 있다
    // 그래서 카운터뿐 아니라 LikeAction 행 자체를 sourcePartyId가 가리키는 PARTY 타깃으로 저장해 파티 직접 좋아요와 완전히 같은 행을 공유하게 한다.
    @Transactional
    public LikeDto likeGoal(long goalId, Member member) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        LikeTarget target = resolveGoalLikeTarget(goal);

        likeActionRepository.save(new LikeAction(member, target.targetType(), target.targetId()));

        int updatedLikeCount;
        if (target.targetType() == TargetType.PARTY) {
            partyRepository.increaseLikeCount(target.targetId());
            updatedLikeCount = partyRepository.findById(target.targetId()).orElseThrow().getLikeCount();
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

        if (target.targetType() == TargetType.PARTY) {
            partyRepository.decreaseLikeCount(target.targetId());
        } else {
            goalRepository.decreaseLikeCount(target.targetId());
        }
    }

    private record LikeTarget(TargetType targetType, long targetId) {
    }

    private LikeTarget resolveGoalLikeTarget(Goal goal) {
        if (goal instanceof Project) {
            return new LikeTarget(TargetType.PARTY, goal.getSourcePartyId());
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

        Map<Long, Long> projectGoalIdToPartyId = new HashMap<>();
        Set<Long> plainGoalIds = new HashSet<>();

        for (Goal goal : goals) {
            if (goal instanceof Project) {
                projectGoalIdToPartyId.put(goal.getId(), goal.getSourcePartyId());
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

        if (!projectGoalIdToPartyId.isEmpty()) {
            Set<Long> partyIds = new HashSet<>(projectGoalIdToPartyId.values());
            Set<Long> likedPartyIds = new HashSet<>(likeActionRepository.findTargetIdsByMemberAndTargetTypeAndTargetIdIn(
                    member, TargetType.PARTY, partyIds
            ));

            // 좋아요된 partyId를 다시 원래 요청받은 goalId로 되돌린다
            for (Map.Entry<Long, Long> entry : projectGoalIdToPartyId.entrySet()) {
                if (likedPartyIds.contains(entry.getValue())) {
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
