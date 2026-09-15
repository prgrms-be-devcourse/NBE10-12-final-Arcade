package com.back.domain.party.showcase.comment.controller;

import com.back.domain.party.showcase.comment.dtos.ShowcaseCommentDto;
import com.back.domain.party.showcase.comment.service.ShowcaseCommentService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/showcase/comments")
@RequiredArgsConstructor
public class ApiV1ShowcaseCommentController {

    private final ShowcaseCommentService showcaseCommentService;
    private final Rq rq;

    @GetMapping
    public RsData<List<ShowcaseCommentDto>> getComments(
            @PathVariable long partyId
    ) {
        return new RsData<>(
                "200-1",
                "댓글 목록 조회 성공",
                showcaseCommentService.getComments(partyId)
        );
    }

    public record WriteReqBody(
            @NotBlank String content,
            Long parentId
    ) {
    }

    @PostMapping
    public RsData<ShowcaseCommentDto> write(
            @PathVariable long partyId,
            @Valid @RequestBody WriteReqBody request
    ) {
        ShowcaseCommentDto dto = showcaseCommentService.write(
                partyId, rq.getActorFromDb(), request.parentId(), request.content());

        return new RsData<>(
                "201-1",
                "댓글 작성 성공",
                dto
        );
    }

    public record EditReqBody(
            @NotBlank String content
    ) {
    }

    @PutMapping("/{commentId}")
    public RsData<Void> edit(
            @PathVariable long partyId,
            @PathVariable long commentId,
            @Valid @RequestBody EditReqBody request
    ) {
        showcaseCommentService.edit(partyId, commentId, rq.getActorFromDb(), request.content());

        return new RsData<>(
                "200-1",
                "댓글 수정 성공",
                null
        );
    }

    @DeleteMapping("/{commentId}")
    public RsData<Void> delete(
            @PathVariable long partyId,
            @PathVariable long commentId
    ) {
        showcaseCommentService.delete(partyId, commentId, rq.getActorFromDb());

        return new RsData<>(
                "200-1",
                "댓글 삭제 성공",
                null
        );
    }
}
