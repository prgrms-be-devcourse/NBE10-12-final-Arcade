package com.back.domain.interaction.bookmark.controller;

import com.back.domain.interaction.bookmark.dtos.BookmarkDto;
import com.back.domain.interaction.bookmark.dtos.MyBookmarkDto;
import com.back.domain.interaction.bookmark.service.BookmarkService;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.dto.PageDto;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class ApiV1BookmarkController {

    private final BookmarkService bookmarkService;
    private final Rq rq;

    @GetMapping("/api/v1/members/me/bookmarks")
    @Operation(
            summary = "내 북마크 정보 조회",
            description = """
                    로그인한 회원이 북마크한 파티·대회·전시를 한 목록에 섞어 최근 담은 순으로 돌려준다.

                    PARTY 는 파티 목록(GET /parties), CONTEST 는 대회 허브(GET /contests),
                    PARTY_SHOWCASE 는 전시관(GET /showcase/goals) 의 항목과 같은 필드를 갖는다.

                    대상이 삭제됐거나 전시가 내려간 북마크는 그릴 카드가 없어 목록에서 빠진다 -
                    거르는 시점이 페이지를 자른 뒤라 그 페이지만 size 보다 짧게 나올 수 있고,
                    totalElements 는 거르기 전 개수다.

                    예외
                    - 400-1 : page·size 범위 검증 실패
                    - 401-1 : 미로그인
                    """
    )
    public RsData<PageDto<MyBookmarkDto>> getMyBookmarks(
            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 1~100, 기본값 20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new RsData<>(
                "200-1",
                "내 북마크 정보 조회 성공",
                bookmarkService.getMyBookmarks(rq.getActorFromDb(), page, size)
        );
    }

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

    @PostMapping("/api/v1/goals/{goal-id}/bookmarks")
    public RsData<BookmarkDto> bookmarkGoal(
            @PathVariable("goal-id") long goalId
    ) {
        BookmarkDto dto = bookmarkService.bookmarkGoal(goalId, rq.getActorFromDb());

        return new RsData<>("201-1", "전시 성취 북마크 성공", dto);
    }

    @DeleteMapping("/api/v1/goals/{goal-id}/bookmarks")
    public RsData<Void> unbookmarkGoal(
            @PathVariable("goal-id") long goalId
    ) {
        bookmarkService.unbookmarkGoal(goalId, rq.getActorFromDb());

        return new RsData<>("204-1", "전시 성취 북마크 취소 성공", null);
    }
}
