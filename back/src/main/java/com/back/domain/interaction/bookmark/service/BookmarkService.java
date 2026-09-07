package com.back.domain.interaction.bookmark.service;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.interaction.bookmark.dtos.BookmarkDto;
import com.back.domain.interaction.bookmark.dtos.MyBookmarkDto;
import com.back.domain.interaction.bookmark.entity.Bookmark;
import com.back.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.dto.PageDto;
import com.back.domain.showcase.showcase.service.ShowcaseService;
import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService implements BookmarkInteractionPort {

    private final BookmarkRepository bookmarkRepository;
    private final ActivityLogService activityLogService;
    private final ContestRepository contestRepository;
    private final ContestPostRepository contestPostRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final GoalRepository goalRepository;
    private final ShowcaseService showcaseService;

    /**
     * 북마크함 조회. 파티·대회·전시를 한 목록에 섞어 최근 담은 순으로 돌려준다(기획서 2.11).
     *
     * Bookmark 는 targetType + targetId 다형성 저장이라 FK 조인으로 한 번에 읽을 수 없다.
     * 그래서 타입별로 id 를 모아 세 번에 나눠 읽고 다시 북마크 순서로 맞춘다.
     *
     * 대상이 사라진 행을 거르는 건 페이지를 자른 뒤라, 그런 행이 섞이면 그 페이지만 요청한
     * size 보다 짧게 나온다(totalElements 는 거르기 전 개수다). 화면은 hasNext 가 아니라
     * totalPages 로 넘기므로 페이지가 밀리지는 않는다.
     */
    public PageDto<MyBookmarkDto> getMyBookmarks(Member actor, int page, int size) {
        Page<Bookmark> bookmarks = bookmarkRepository.findAllByMemberOrderByCreateDateDesc(
                actor, PageRequest.of(page, size));

        Map<TargetType, List<Long>> idsByType = bookmarks.getContent().stream()
                .collect(Collectors.groupingBy(
                        Bookmark::getTargetType,
                        Collectors.mapping(Bookmark::getTargetId, Collectors.toList())));

        Map<TargetType, Map<Long, Object>> cards = Map.of(
                TargetType.PARTY, partyCards(idsByType.getOrDefault(TargetType.PARTY, List.of())),
                TargetType.CONTEST, contestCards(idsByType.getOrDefault(TargetType.CONTEST, List.of())),
                TargetType.GOAL, goalCards(idsByType.getOrDefault(TargetType.GOAL, List.of()))
        );

        // 대상이 사라졌거나 전시가 내려간 북마크는 그릴 카드가 없어 건너뛴다.
        // 삭제 시 deleteAllBookmarksForTarget 이 정리하지만, 놓친 행이 500 을 내지 않게 한다.
        List<MyBookmarkDto> content = bookmarks.getContent().stream()
                .filter(bookmark -> cards.get(bookmark.getTargetType()).containsKey(bookmark.getTargetId()))
                .map(bookmark -> new MyBookmarkDto(
                        bookmark.getId(),
                        bookmark.getTargetType(),
                        cards.get(bookmark.getTargetType()).get(bookmark.getTargetId()),
                        bookmark.getCreateDate()))
                .toList();

        return new PageDto<>(content, bookmarks.getNumber(), bookmarks.getSize(),
                bookmarks.getTotalElements(), bookmarks.getTotalPages());
    }

    private Map<Long, Object> partyCards(List<Long> partyIds) {
        // 두 조회 다 @Query 라 빈 목록이어도 그대로 실행된다. 북마크함은 타입 3종을 항상 다 부르므로
        // 대개 2종이 비어 있어, 여기서 끊지 않으면 매 요청 헛도는 쿼리가 생긴다.
        if (partyIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> applicantCounts = partyMemberRepository.countApplicantsByPartyIds(partyIds);

        return partyRepository.findAllByIdIn(partyIds).stream()
                .collect(Collectors.toMap(
                        Party::getId,
                        party -> new PartyListItemDto(party, applicantCounts.getOrDefault(party.getId(), 0L))));
    }

    private Map<Long, Object> contestCards(List<Long> contestIds) {
        if (contestIds.isEmpty()) {
            return Map.of();
        }

        // 접수기간이 지난 대회는 ContestPost 가 지워질 수 있다 - 그때는 null 을 넘겨 archived 카드로 조립된다.
        Map<Long, ContestPost> posts = contestPostRepository.findAllByContestIdIn(contestIds).stream()
                .collect(Collectors.toMap(post -> post.getContest().getId(), post -> post));

        return contestRepository.findAllById(contestIds).stream()
                .collect(Collectors.toMap(
                        contest -> contest.getId(),
                        contest -> new ContestResponseDto(contest, posts.get(contest.getId()))));
    }

    private Map<Long, Object> goalCards(List<Long> goalIds) {
        // 여기는 빈 목록 가드가 필요 없다 - findAllById 는 Spring Data 가 비어 있으면
        // 쿼리 없이 emptyList 를 돌려준다(SimpleJpaRepository.findAllById).
        return goalRepository.findAllById(goalIds).stream()
                .filter(Goal::isExhibited)
                .collect(Collectors.toMap(Goal::getId, showcaseService::toDto));
    }

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
        activityLogService.record(member);

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
