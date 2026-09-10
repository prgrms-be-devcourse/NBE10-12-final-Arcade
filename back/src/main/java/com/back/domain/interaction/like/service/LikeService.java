package com.back.domain.interaction.like.service;

import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.interaction.like.dtos.LikeDto;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
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

    public boolean isGoalLiked(long goalId, Member member) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        return isLiked(member, TargetType.PARTY_SHOWCASE, requireShowcaseId(goal));
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

    @Transactional
    public LikeDto likeGoal(long goalId, Member member) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        long showcaseId = requireShowcaseId(goal);

        likeActionRepository.save(new LikeAction(member, TargetType.PARTY_SHOWCASE, showcaseId));
        partyShowcaseRepository.increaseLikeCount(showcaseId);

        int updatedLikeCount = partyShowcaseRepository.findById(showcaseId).orElseThrow().getLikeCount();
        return new LikeDto(TargetType.GOAL, goalId, true, updatedLikeCount);
    }

    @Transactional
    public void unlikeGoal(long goalId, Member member) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        long showcaseId = requireShowcaseId(goal);

        likeActionRepository.deleteByMemberAndTargetTypeAndTargetId(member, TargetType.PARTY_SHOWCASE, showcaseId);
        partyShowcaseRepository.decreaseLikeCount(showcaseId);
    }

    // 컨트롤러가 goalExists(=isExhibited) 가드를 이미 통과시켰으므로 게시된 PROJECT만 도달한다.
    private long requireShowcaseId(Goal goal) {
        if (goal instanceof Project project && project.getPartyShowcase() != null) {
            return project.getPartyShowcase().getId();
        }
        throw new IllegalStateException("전시된 PROJECT 성취만 좋아요 대상입니다: goalId=" + goal.getId());
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
        return new HashSet<>(likeActionRepository.findTargetIdsByMemberAndTargetTypeAndTargetIdIn(member, targetType, targetIds));
    }

    private ContestPost findContestPostOrThrow(long contestId) {
        return contestPostRepository.findByContestId(contestId).orElseThrow();
    }
}
