package com.back.domain.party.application.controller;

import com.back.domain.party.application.dtos.PartyApplicationDto;
import com.back.domain.party.application.service.PartyApplicationService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/applications")
@RequiredArgsConstructor
public class ApiV1PartyApplicationController {

    private final PartyApplicationService partyApplicationService;
    private final Rq rq;

    public record ApplyReqBody(
            @NotNull Long positionId,
            @Size(max = 50, message = "지원 메시지는 50자를 초과할 수 없습니다.") String message
    ) { }

    @PostMapping
    public RsData<PartyApplicationDto> apply(
            @PathVariable long partyId,
            @Valid @RequestBody ApplyReqBody request
    ) {
        PartyApplicationDto dto = partyApplicationService.apply(
                partyId,
                request.positionId(),
                rq.getActorFromDb(),
                request.message()
        );

        return new RsData<>(
                "201-1",
                "지원 성공",
                dto
        );
    }

    @GetMapping
    public RsData<List<PartyApplicationDto>> list(
            @PathVariable long partyId
    ) {
        List<PartyApplicationDto> applications = partyApplicationService.getApplications(
                partyId,
                rq.getActorFromDb()
        );
        return new RsData<>(
                "200-1",
                "지원자 목록 조회 성공",
                applications
        );
    }
}
