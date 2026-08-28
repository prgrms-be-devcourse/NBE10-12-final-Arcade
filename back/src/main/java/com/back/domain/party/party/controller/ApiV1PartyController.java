package com.back.domain.party.party.controller;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.service.PartyService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class ApiV1PartyController {

    private final PartyService partyService;
    private final Rq rq;

    public record PositionReqBody(
        @NotNull PositionType name,
        int capacity
    ) { }

    public record PartyCreateReqBody(
        @NotNull String partyName,
        @NotNull String title,
        String description,
        Long targetContestId,
        String contestName,
        String contestLinkUrl,
        @NotNull TopicType topicType,
        @NotNull PartyTag partyTag,
        String githubRepoUrl,
        int checklistRequiredApprovals,
        @NotNull LocalDateTime deadline,
        @NotEmpty List<@Valid PositionReqBody> positions
    ) { }

    @PostMapping
    public RsData<PartyDto> create(
        @Valid @RequestBody PartyCreateReqBody request
    ) {
        List<PartyService.PositionCreateSpec> positionSpecs = request.positions().stream()
            .map(p -> new PartyService.PositionCreateSpec(p.name(), p.capacity()))
            .toList();

        PartyDto partyDto = partyService.create(
            rq.getActorFromDb(),
            request.partyName(),
            request.title(),
            request.description(),
            request.targetContestId(),
            request.contestName(),
            request.contestLinkUrl(),
            request.topicType(),
            request.partyTag(),
            request.githubRepoUrl(),
            request.checklistRequiredApprovals(),
            request.deadline(),
            positionSpecs
        );

        return new RsData<>(
            "201-1",
            "파티 생성 성공",
            partyDto
        );
    }
    public record PositionCapacityReqBody(
            @NotNull Long positionId,
            int capacity
    ) { }

    public record PartyUpdateReqBody(
            @NotNull String partyName,
            @NotNull String title,
            String description,
            Long targetContestId,
            String contestName,
            String contestLinkUrl,
            @NotNull TopicType topicType,
            @NotNull PartyTag partyTag,
            String githubRepoUrl,
            @NotNull LocalDateTime deadline,
            List<@Valid PositionCapacityReqBody> positions
    ) { }

    @PatchMapping("/{party-id}")
    public RsData<PartyDto> update(
            @PathVariable("party-id") long partyId,
            @Valid @RequestBody PartyUpdateReqBody request
    ) {
        List<PartyService.PositionCapacityUpdateSpec> positionSpecs = request.positions() == null
                ? List.of()
                : request.positions().stream()
                .map(p -> new PartyService.PositionCapacityUpdateSpec(p.positionId(), p.capacity()))
                .toList();

        PartyDto partyDto = partyService.update(
                partyId,
                rq.getActorFromDb(),
                request.partyName(),
                request.title(),
                request.description(),
                request.targetContestId(),
                request.contestName(),
                request.contestLinkUrl(),
                request.topicType(),
                request.partyTag(),
                request.githubRepoUrl(),
                request.deadline(),
                positionSpecs
        );

        return new RsData<>(
                "200-1",
                "파티 정보 수정 성공",
                partyDto
        );
    }

    @DeleteMapping("/{party-id}")
    public RsData<Void> delete(
            @PathVariable("party-id") long partyId
    ) {
        partyService.delete(partyId, rq.getActorFromDb());

        return new RsData<>(
                "204-1",
                "파티 삭제 성공",
                null
        );
    }
}
