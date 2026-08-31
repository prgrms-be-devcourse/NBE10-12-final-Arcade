package com.back.domain.contest.contest.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestSortOption;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/contests")
@RequiredArgsConstructor
@Tag(name = "ApiV1ContestController", description = "대회 글 컨트롤러")
public class ApiV1ContestController {
    private final ContestService contestService;
    private final Rq rq;

    public record ContestWriteReqBody(
            @NotBlank
            @Size(min =5, max= 25)
            String title,
            @NotNull
            ContestFormat format,
            @NotNull
            ContestTag contestTag,
            @NotNull
            LocalDate applicationPeriodStart,
            @NotNull
            LocalDate applicationPeriodEnd,
            @Size(min = 10, max = 20000)
            String description,
            String imageUrl,
            @NotBlank
            String linkUrl
    ) { }

    @PostMapping
    @Operation(summary = "대회글 작성")
    public RsData<ContestResponseDto> write(
            @RequestBody @Valid ContestWriteReqBody reqBody
    ){
        Member actor = rq.getActorFromDb();
        checkPeriod(reqBody.applicationPeriodStart(), reqBody.applicationPeriodEnd());

        ContestResponseDto contestResponseDto = contestService.write(
                actor,
                reqBody.title(),
                reqBody.format(),
                reqBody.contestTag(),
                reqBody.applicationPeriodStart(),
                reqBody.applicationPeriodEnd(),
                reqBody.description(),
                reqBody.linkUrl(),
                reqBody.imageUrl()
        );

        return new RsData<>(
                "201-1",
                "공모전 등록 성공",
                contestResponseDto
        );
    }

    public record ContestModifyReqBody(
            @NotBlank
            @Size(min = 5, max = 25)
            String title,
            @Size(min = 10, max = 20000)
            String description,
            @NotNull
            LocalDate applicationPeriodStart,
            @NotNull
            LocalDate applicationPeriodEnd,
            @NotBlank
            String linkUrl,
            String imageUrl
    ) { }

    @PatchMapping("/{contest-id}")
    @Operation(summary = "대회글 수정")
    public RsData<ContestResponseDto> modify(
            @PathVariable("contest-id") long contestId,
            @RequestBody @Valid ContestModifyReqBody reqBody
    ) {
        checkPeriod(reqBody.applicationPeriodStart(), reqBody.applicationPeriodEnd());

        ContestResponseDto contestResponseDto = contestService.modify(
                contestId,
                reqBody.title(),
                reqBody.description(),
                reqBody.applicationPeriodStart(),
                reqBody.applicationPeriodEnd(),
                reqBody.linkUrl(),
                reqBody.imageUrl()
        );

        return new RsData<>(
                "200-1",
                "대회 수정 성공",
                contestResponseDto
        );
    }

    @GetMapping
    @Operation(summary = "대회 목록 조회")
    public RsData<Page<ContestResponseDto>> list(
            @RequestParam(required = false) ContestFormat format,
            @RequestParam(required = false) ContestTag contestTag,
            @RequestParam(defaultValue = "LATEST") ContestSortOption sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ContestResponseDto> contests = contestService.list(
                format,
                contestTag,
                sort,
                PageRequest.of(page, size)
        );

        return new RsData<>(
                "200-1",
                "대회 목록 조회 성공",
                contests
        );
    }

    private static final int VIEW_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24;

    @GetMapping("/{contest-id}")
    @Operation(summary = "대회 상세 조회")
    public RsData<ContestResponseDto> getDetail(@PathVariable("contest-id") long contestId) {
        String viewCookieName = "contest_viewed_" + contestId;
        boolean alreadyViewed = rq.getCookieValue(viewCookieName, null) != null;

        ContestResponseDto contestResponseDto = contestService.getDetail(contestId, !alreadyViewed).orElseThrow();

        if (!alreadyViewed) {
            rq.setCookie(viewCookieName, "true", VIEW_COOKIE_MAX_AGE_SECONDS);
        }

        return new RsData<>(
                "200-1",
                "대회 상세 조회 성공",
                contestResponseDto
        );
    }

    @DeleteMapping("/{contest-id}")
    @Operation(summary = "대회글 삭제")
    public RsData<Void> delete(@PathVariable("contest-id") long contestId) {
        contestService.deletePost(contestId);

        return new RsData<>(
                "204-1",
                "대회 게시글 삭제 성공"
        );
    }

    private void checkPeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new ServiceException("400-3", "모집 시작일은 종료일보다 이후일 수 없습니다.");
        }
    }

}
