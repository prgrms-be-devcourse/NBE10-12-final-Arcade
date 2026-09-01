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
        BookmarkDto dto = bookmarkService.bookmarkContest(contestId, rq.getActorFromDb());

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
        bookmarkService.unbookmarkContest(contestId, rq.getActorFromDb());

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
        BookmarkDto dto = bookmarkService.bookmarkParty(partyId, rq.getActorFromDb());

        return new RsData<>("201-1", "파티 북마크 성공", dto);
    }

    @DeleteMapping("/api/v1/parties/{partyId}/bookmarks")
    public RsData<Void> unbookmarkParty(
            @PathVariable long partyId
    ) {
        bookmarkService.unbookmarkParty(partyId, rq.getActorFromDb());

        return new RsData<>("204-1", "파티 북마크 취소 성공", null);
    }
}
