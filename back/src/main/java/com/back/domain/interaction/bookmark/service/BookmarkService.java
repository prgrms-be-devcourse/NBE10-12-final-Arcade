package com.back.domain.interaction.bookmark.service;

import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.interaction.bookmark.dtos.BookmarkDto;
import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ContestRepository contestRepository;
    private final ContestPostRepository contestPostRepository;

    @Transactional
    public BookmarkDto bookmarkContest(long contestId, Member member) {
        findContestPostOrThrow(contestId);

        if (bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(member, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "이미 북마크한 대회입니다.");
        }

        bookmarkRepository.save(new Bookmark(member, TargetType.CONTEST, contestId));

        return new BookmarkDto(TargetType.CONTEST, contestId, true);
    }

    @Transactional
    public void unbookmarkContest(long contestId, Member member) {
        findContestPostOrThrow(contestId);

        Bookmark bookmark = bookmarkRepository
                .findByMemberAndTargetTypeAndTargetId(member, TargetType.CONTEST, contestId)
                .orElseThrow(() -> new ServiceException("409-1", "북마크하지 않은 대회입니다."));

        bookmarkRepository.delete(bookmark);
    }

    @Transactional
    public void deleteAllBookmarksForContest(long contestId) {
        bookmarkRepository.deleteAllByTargetTypeAndTargetId(TargetType.CONTEST, contestId);
    }

    public Set<Long> findBookmarkedTargetIds(Member member, TargetType targetType) {
        if (member == null) {
            return Set.of();
        }

        return new HashSet<>(bookmarkRepository.findTargetIdsByMemberAndTargetType(member, targetType));
    }

    private ContestPost findContestPostOrThrow(long contestId) {
        var contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 대회입니다."));

        return contestPostRepository.findByContest(contest)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 대회입니다."));
    }
}
