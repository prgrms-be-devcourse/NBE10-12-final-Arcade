package com.back.domain.party.party.controller;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.PartySortOption;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.service.PartyLifecycleService;
import com.back.domain.party.party.service.PartyService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class ApiV1PartyController {

    private final PartyService partyService;
    private final PartyLifecycleService partyLifecycleService;
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

    @PatchMapping("/{partyId}")
    public RsData<PartyDto> update(
            @PathVariable long partyId,
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

    @DeleteMapping("/{partyId}")
    public RsData<Void> delete(
            @PathVariable long partyId
    ) {
        partyService.delete(partyId, rq.getActorFromDb());

        return new RsData<>(
                "204-1",
                "파티 삭제 성공",
                null
        );
    }

    @GetMapping
    public RsData<Page<PartyListItemDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PartyTag partyTag,
            @RequestParam(required = false) PositionType position,
            @RequestParam(defaultValue = "DEADLINE") PartySortOption sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PartyListItemDto> parties = partyService.getList(
                keyword,
                partyTag,
                position,
                sort,
                PageRequest.of(page, size)
        );

        return new RsData<>(
                "200-1",
                "파티 목록 조회 성공",
                parties
        );
    }

    @GetMapping("/{partyId}")
    public RsData<PartyDto> detail(
            @PathVariable long partyId
    ) {
        PartyDto partyDto = partyService.getDetail(partyId);

        return new RsData<>(
                "200-1",
                "파티 상세 조회 성공",
                partyDto
        );
    }

    @PostMapping("/{partyId}/close-recruiting")
    public RsData<PartyDto> closeRecruiting(
            @PathVariable long partyId
    ) {
        PartyDto partyDto = partyLifecycleService.closeRecruiting(partyId, rq.getActorFromDb());

        return new RsData<>(
                "201-1",
                "파티 모집 종료",
                partyDto
        );
    }

    @PostMapping("/{partyId}/complete")
    public RsData<PartyDto> complete(
            @PathVariable long partyId
    ) {
        PartyDto partyDto = partyLifecycleService.complete(partyId, rq.getActorFromDb());

        return new RsData<>(
                "200-1",
                "파티 완료 처리 성공",
                partyDto
        );
    }
}
