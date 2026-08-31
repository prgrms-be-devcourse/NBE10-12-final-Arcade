package com.back.domain.interaction.like.controller;

import com.back.domain.interaction.like.dtos.LikeDto;
import com.back.domain.interaction.like.service.LikeService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiV1LikeController {

    private final LikeService likeService;
    private final Rq rq;

    @PostMapping("/api/v1/parties/{party-id}/likes")
    public RsData<LikeDto> likeParty(
            @PathVariable("party-id") long partyId
    ) {
        LikeDto dto = likeService.likeParty(partyId, rq.getActorFromDb());

        return new RsData<>(
                "201-1",
                "파티 좋아요 성공",
                dto
        );
    }

    @DeleteMapping("/api/v1/parties/{party-id}/likes")
    public RsData<Void> unlikeParty(
            @PathVariable("party-id") long partyId
    ) {
        likeService.unlikeParty(partyId, rq.getActorFromDb());

        return new RsData<>(
                "204-1",
                "파티 좋아요 취소 성공",
                null
        );
    }

    @PostMapping("/api/v1/contests/{contest-id}/likes")
    public RsData<LikeDto> likeContest(
            @PathVariable("contest-id") long contestId
    ) {
        LikeDto dto = likeService.likeContest(contestId, rq.getActorFromDb());

        return new RsData<>(
                "201-1",
                "대회 좋아요 성공",
                dto
        );
    }

    @DeleteMapping("/api/v1/contests/{contest-id}/likes")
    public RsData<Void> unlikeContest(
            @PathVariable("contest-id") long contestId
    ) {
        likeService.unlikeContest(contestId, rq.getActorFromDb());

        return new RsData<>(
                "204-1",
                "대회 좋아요 취소 성공",
                null
        );
    }
}
