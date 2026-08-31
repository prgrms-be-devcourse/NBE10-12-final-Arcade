package com.back.domain.interaction.bookmark.service;

import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.interaction.bookmark.dtos.BookmarkDto;
import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService implements BookmarkInteractionPort {

    private final BookmarkRepository bookmarkRepository;
    private final ContestRepository contestRepository;
    private final ContestPostRepository contestPostRepository;

    public boolean contestPostExists(long contestId) {
        return contestRepository.findById(contestId)
                .flatMap(contestPostRepository::findByContest)
                .isPresent();
    }

    public boolean isBookmarked(Member member, TargetType targetType, long targetId) {
        return bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(member, targetType, targetId);
    }

    @Transactional
    public BookmarkDto bookmarkContest(long contestId, Member member) {
        bookmarkRepository.save(new Bookmark(member, TargetType.CONTEST, contestId));

        return new BookmarkDto(TargetType.CONTEST, contestId, true);
    }

    @Transactional
    public void unbookmarkContest(long contestId, Member member) {
        Bookmark bookmark = bookmarkRepository
                .findByMemberAndTargetTypeAndTargetId(member, TargetType.CONTEST, contestId)
                .orElseThrow();

        bookmarkRepository.delete(bookmark);
    }

    @Override
    @Transactional
    public void deleteAllBookmarksForTarget(TargetType targetType, long targetId) {
        bookmarkRepository.deleteAllByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    public Set<Long> findBookmarkedTargetIds(Member member, TargetType targetType) {
        if (member == null) {
            return Set.of();
        }

        return new HashSet<>(bookmarkRepository.findTargetIdsByMemberAndTargetType(member, targetType));
    }
}
