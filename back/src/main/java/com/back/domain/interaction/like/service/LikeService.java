package com.back.domain.interaction.like.service;

import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.interaction.like.dtos.LikeDto;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeActionRepository likeActionRepository;
    private final PartyRepository partyRepository;
    private final ContestRepository contestRepository;
    private final ContestPostRepository contestPostRepository;

    @Transactional
    public LikeDto likeParty(long partyId, Member member) {
        findPartyOrThrow(partyId);

        if (likeActionRepository.existsByMemberAndTargetTypeAndTargetId(member, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "이미 좋아요한 파티입니다.");
        }

        likeActionRepository.save(new LikeAction(member, TargetType.PARTY, partyId));
        partyRepository.increaseLikeCount(partyId);

        int updatedLikeCount = findPartyOrThrow(partyId).getLikeCount();
        return new LikeDto(TargetType.PARTY, partyId, true, updatedLikeCount);
    }

    @Transactional
    public void unlikeParty(long partyId, Member member) {
        findPartyOrThrow(partyId);

        LikeAction likeAction = likeActionRepository
                .findByMemberAndTargetTypeAndTargetId(member, TargetType.PARTY, partyId)
                .orElseThrow(() -> new ServiceException("409-1", "좋아요하지 않은 파티입니다."));

        likeActionRepository.delete(likeAction);
        partyRepository.decreaseLikeCount(partyId);
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }

    @Transactional
    public LikeDto likeContest(long contestId, Member member) {
        findContestPostOrThrow(contestId);

        if (likeActionRepository.existsByMemberAndTargetTypeAndTargetId(member, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "이미 좋아요한 대회입니다.");
        }

        likeActionRepository.save(new LikeAction(member, TargetType.CONTEST, contestId));
        contestPostRepository.increaseLikeCount(contestId);

        int updatedLikeCount = findContestPostOrThrow(contestId).getLikeCount();
        return new LikeDto(TargetType.CONTEST, contestId, true, updatedLikeCount);
    }

    @Transactional
    public void unlikeContest(long contestId, Member member) {
        findContestPostOrThrow(contestId);

        LikeAction likeAction = likeActionRepository
                .findByMemberAndTargetTypeAndTargetId(member, TargetType.CONTEST, contestId)
                .orElseThrow(() -> new ServiceException("409-1", "좋아요하지 않은 대회입니다."));

        likeActionRepository.delete(likeAction);
        contestPostRepository.decreaseLikeCount(contestId);
    }

    @Transactional
    public void deleteAllLikesForContest(long contestId) {
        likeActionRepository.deleteAllByTargetTypeAndTargetId(TargetType.CONTEST, contestId);
    }

    public Set<Long> findLikedTargetIds(Member member, TargetType targetType) {
        if (member == null) {
            return Set.of();
        }

        return new HashSet<>(likeActionRepository.findTargetIdsByMemberAndTargetType(member, targetType));
    }

    private ContestPost findContestPostOrThrow(long contestId) {
        var contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 대회입니다."));

        return contestPostRepository.findByContest(contest)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 대회입니다."));
    }
}
