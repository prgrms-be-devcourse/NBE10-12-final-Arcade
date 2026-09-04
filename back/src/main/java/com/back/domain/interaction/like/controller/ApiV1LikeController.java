package com.back.domain.interaction.like.controller;

import com.back.domain.interaction.like.dtos.LikeDto;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.service.LikeService;
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
public class ApiV1LikeController {

    private final LikeService likeService;
    private final Rq rq;

    @PostMapping("/api/v1/parties/{partyId}/likes")
    public RsData<LikeDto> likeParty(
            @PathVariable long partyId
    ) {
        Member actor = rq.getActorFromDb();

        if (!likeService.partyExists(partyId)) {
            throw new ServiceException("404-1", "존재하지 않는 파티입니다.");
        }
        if (likeService.isLiked(actor, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "이미 좋아요한 파티입니다.");
        }

        LikeDto dto = likeService.likeParty(partyId, actor);

        return new RsData<>(
                "201-1",
                "파티 좋아요 성공",
                dto
        );
    }

    @DeleteMapping("/api/v1/parties/{partyId}/likes")
    public RsData<Void> unlikeParty(
            @PathVariable long partyId
    ) {
        Member actor = rq.getActorFromDb();

        if (!likeService.partyExists(partyId)) {
            throw new ServiceException("404-1", "존재하지 않는 파티입니다.");
        }
        if (!likeService.isLiked(actor, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "좋아요하지 않은 파티입니다.");
        }

        likeService.unlikeParty(partyId, actor);

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
        Member actor = rq.getActorFromDb();

        if (!likeService.contestPostExists(contestId)) {
            throw new ServiceException("404-1", "존재하지 않는 대회입니다.");
        }
        if (likeService.isLiked(actor, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "이미 좋아요한 대회입니다.");
        }

        LikeDto dto = likeService.likeContest(contestId, actor);

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
        Member actor = rq.getActorFromDb();

        if (!likeService.contestPostExists(contestId)) {
            throw new ServiceException("404-1", "존재하지 않는 대회입니다.");
        }
        if (!likeService.isLiked(actor, TargetType.CONTEST, contestId)) {
            throw new ServiceException("409-1", "좋아요하지 않은 대회입니다.");
        }

        likeService.unlikeContest(contestId, actor);

        return new RsData<>(
                "204-1",
                "대회 좋아요 취소 성공",
                null
        );
    }

    @PostMapping("/api/v1/goals/{goal-id}/likes")
    public RsData<LikeDto> likeGoal(
            @PathVariable("goal-id") long goalId
    ) {
        Member actor = rq.getActorFromDb();

        if (!likeService.goalExists(goalId)) {
            throw new ServiceException("404-1", "존재하지 않는 성취입니다.");
        }
        if (likeService.isLiked(actor, TargetType.GOAL, goalId)) {
            throw new ServiceException("409-1", "이미 좋아요한 성취입니다.");
        }

        LikeDto dto = likeService.likeGoal(goalId, actor);

        return new RsData<>(
                "201-1",
                "좋아요 성공",
                dto
        );
    }

    @DeleteMapping("/api/v1/goals/{goal-id}/likes")
    public RsData<Void> unlikeGoal(
            @PathVariable("goal-id") long goalId
    ) {
        Member actor = rq.getActorFromDb();

        if (!likeService.goalExists(goalId)) {
            throw new ServiceException("404-1", "존재하지 않는 성취입니다.");
        }
        if (!likeService.isLiked(actor, TargetType.GOAL, goalId)) {
            throw new ServiceException("409-1", "좋아요하지 않은 성취입니다.");
        }

        likeService.unlikeGoal(goalId, actor);

        return new RsData<>(
                "204-1",
                "좋아요 취소 성공",
                null
        );
    }
}
