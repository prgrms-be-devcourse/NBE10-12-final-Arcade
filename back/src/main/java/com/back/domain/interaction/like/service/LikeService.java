package com.back.domain.interaction.like.service;

import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.interaction.like.dtos.LikeDto;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.repository.PartyRepository;
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
    private final PartyRepository partyRepository;
    private final ContestPostRepository contestPostRepository;

    public boolean partyExists(long partyId) {
        return partyRepository.existsById(partyId);
    }

    public boolean contestPostExists(long contestId) {
        return contestPostRepository.existsByContestId(contestId);
    }

    public boolean isLiked(Member member, TargetType targetType, long targetId) {
        return likeActionRepository.existsByMemberAndTargetTypeAndTargetId(member, targetType, targetId);
    }

    @Transactional
    public LikeDto likeParty(long partyId, Member member) {
        likeActionRepository.save(new LikeAction(member, TargetType.PARTY, partyId));
        partyRepository.increaseLikeCount(partyId);

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
