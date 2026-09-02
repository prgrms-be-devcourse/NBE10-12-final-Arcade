package com.back.domain.party.showcase.controller;

import com.back.domain.party.showcase.dtos.PartyShowcaseDto;
import com.back.domain.party.showcase.service.PartyShowcaseService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/showcase")
@RequiredArgsConstructor
public class ApiV1PartyShowcaseController {

    private final PartyShowcaseService partyShowcaseService;
    private final Rq rq;

    @GetMapping
    public RsData<PartyShowcaseDto> getDraft(
            @PathVariable long partyId
    ) {
        PartyShowcaseDto dto = partyShowcaseService.getDraft(partyId);

        return new RsData<>(
                "200-1",
                "전시 게시 초안 조회 성공",
                dto
        );
    }

    public record PublishReqBody(
            @NotNull String title,
            String description
    ) { }

    @PostMapping
    public RsData<PartyShowcaseDto> publish(
            @PathVariable long partyId,
            @Valid @RequestBody PublishReqBody request
    ) {
        PartyShowcaseDto dto = partyShowcaseService.publish(
                partyId,
                rq.getActorFromDb(),
                request.title(),
                request.description()
        );

        return new RsData<>(
                "201-1",
                "전시 게시 성공",
                dto
        );
    }
}
