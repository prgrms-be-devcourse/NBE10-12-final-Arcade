package com.back.domain.interaction.bookmark.service;

import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.interaction.bookmark.dtos.BookmarkDto;
import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService implements BookmarkInteractionPort {

    private final BookmarkRepository bookmarkRepository;
    private final ContestPostRepository contestPostRepository;
    private final PartyRepository partyRepository;
    private final GoalRepository goalRepository;

    public boolean isBookmarked(Member member, TargetType targetType, long targetId) {
        return bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(member, targetType, targetId);
    }

    @Transactional
    public BookmarkDto bookmarkContest(long contestId, Member member) {
        if (!contestPostRepository.existsByContestId(contestId)) {
            throw new ServiceException("404-1", "존재하지 않는 대회입니다.");
        }
        if (isBookmarked(member, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "이미 북마크한 대회입니다.");
        }

        bookmarkRepository.save(new Bookmark(member, TargetType.CONTEST, contestId));

        return new BookmarkDto(TargetType.CONTEST, contestId, true);
    }

    @Transactional
    public void unbookmarkContest(long contestId, Member member) {
        if (!contestPostRepository.existsByContestId(contestId)) {
            throw new ServiceException("404-1", "존재하지 않는 대회입니다.");
        }
        if (!isBookmarked(member, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "북마크하지 않은 대회입니다.");
        }

        bookmarkRepository.deleteByMemberAndTargetTypeAndTargetId(member, TargetType.CONTEST, contestId);
    }

    @Override
    @Transactional
    public void deleteAllBookmarksForTarget(TargetType targetType, long targetId) {
        bookmarkRepository.deleteAllByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public Set<Long> findBookmarkedTargetIds(Member member, TargetType targetType, Collection<Long> targetIds) {
        if (member == null || targetIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(bookmarkRepository.findTargetIdsByMemberAndTargetTypeAndTargetIdIn(member, targetType, targetIds));
    }

    @Transactional
    public BookmarkDto bookmarkParty(long partyId, Member member) {
        if (!partyRepository.existsById(partyId)) {
            throw new ServiceException("404-1", "존재하지 않는 파티입니다.");
        }
        if (isBookmarked(member, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "이미 북마크한 파티입니다.");
        }

        bookmarkRepository.save(new Bookmark(member, TargetType.PARTY, partyId));

        return new BookmarkDto(TargetType.PARTY, partyId, true);
    }

    @Transactional
    public void unbookmarkParty(long partyId, Member member) {
        if (!partyRepository.existsById(partyId)) {
            throw new ServiceException("404-1", "존재하지 않는 파티입니다.");
        }
        if (!isBookmarked(member, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "북마크하지 않은 파티입니다.");
        }

        bookmarkRepository.deleteByMemberAndTargetTypeAndTargetId(member, TargetType.PARTY, partyId);
    }

    // 북마크 가능은 존재 여부뿐 아니라 전시 여부까지 포함한다.
    // 아직 전시 안 된 성취를 외부에 굳이 알릴 필요 없어서 못 찾은 것과 같은 404로 묶는다.
    private boolean isGoalExhibited(long goalId) {
        return goalRepository.findById(goalId)
                .map(Goal::isExhibited)
                .orElse(false);
    }

    @Transactional
    public BookmarkDto bookmarkGoal(long goalId, Member member) {
        if (!isGoalExhibited(goalId)) {
            throw new ServiceException("404-1", "존재하지 않는 성취입니다.");
        }
        if (isBookmarked(member, TargetType.GOAL, goalId)) {
            throw new ServiceException("409-1", "이미 북마크한 성취입니다.");
        }

        bookmarkRepository.save(new Bookmark(member, TargetType.GOAL, goalId));

        return new BookmarkDto(TargetType.GOAL, goalId, true);
    }

    @Transactional
    public void unbookmarkGoal(long goalId, Member member) {
        if (!isGoalExhibited(goalId)) {
            throw new ServiceException("404-1", "존재하지 않는 성취입니다.");
        }
        if (!isBookmarked(member, TargetType.GOAL, goalId)) {
            throw new ServiceException("409-1", "북마크하지 않은 성취입니다.");
        }

        bookmarkRepository.deleteByMemberAndTargetTypeAndTargetId(member, TargetType.GOAL, goalId);
    }
}
