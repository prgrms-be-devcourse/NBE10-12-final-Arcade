package com.back.domain.interaction.bookmark.controller;

import com.back.domain.interaction.bookmark.dtos.BookmarkDto;
import com.back.domain.interaction.bookmark.service.BookmarkService;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiV1BookmarkController {

    private final BookmarkService bookmarkService;
    private final Rq rq;

    @PostMapping("/api/v1/contests/{contest-id}/bookmarks")
    public RsData<BookmarkDto> bookmarkContest(
            @PathVariable("contest-id") long contestId
    ) {
        Member actor = rq.getActorFromDb();

        if (!bookmarkService.contestPostExists(contestId)) {
            throw new ServiceException("404-1", "존재하지 않는 대회입니다.");
        }
        if (bookmarkService.isBookmarked(actor, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "이미 북마크한 대회입니다.");
        }

        BookmarkDto dto = bookmarkService.bookmarkContest(contestId, actor);

        return new RsData<>(
                "201-1",
                "대회 북마크 성공",
                dto
        );
    }

    @DeleteMapping("/api/v1/contests/{contest-id}/bookmarks")
    public RsData<Void> unbookmarkContest(
            @PathVariable("contest-id") long contestId
    ) {
        Member actor = rq.getActorFromDb();

        if (!bookmarkService.contestPostExists(contestId)) {
            throw new ServiceException("404-1", "존재하지 않는 대회입니다.");
        }
        if (!bookmarkService.isBookmarked(actor, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "북마크하지 않은 대회입니다.");
        }

        bookmarkService.unbookmarkContest(contestId, actor);

        return new RsData<>(
                "204-1",
                "대회 북마크 취소 성공",
                null
        );
    }

    @PostMapping("/api/v1/parties/{partyId}/bookmarks")
    public RsData<BookmarkDto> bookmarkParty(
            @PathVariable long partyId
    ) {
        Member actor = rq.getActorFromDb();

        if (!bookmarkService.partyExists(partyId)) {
            throw new ServiceException("404-1", "존재하지 않는 파티입니다.");
        }
        if (bookmarkService.isBookmarked(actor, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "이미 북마크한 파티입니다.");
        }

        BookmarkDto dto = bookmarkService.bookmarkParty(partyId, actor);

        return new RsData<>("201-1", "파티 북마크 성공", dto);
    }

    @DeleteMapping("/api/v1/parties/{partyId}/bookmarks")
    public RsData<Void> unbookmarkParty(
            @PathVariable long partyId
    ) {
        Member actor = rq.getActorFromDb();

        if (!bookmarkService.partyExists(partyId)) {
            throw new ServiceException("404-1", "존재하지 않는 파티입니다.");
        }
        if (!bookmarkService.isBookmarked(actor, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "북마크하지 않은 파티입니다.");
        }

        bookmarkService.unbookmarkParty(partyId, actor);

        return new RsData<>("204-1", "파티 북마크 취소 성공", null);
    }

}
