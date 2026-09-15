package com.back.domain.party.showcase.controller;

import com.back.domain.party.showcase.dtos.PartyShowcaseDto;
import com.back.domain.party.showcase.service.PartyShowcaseService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApiV1ShowcaseRankingController {

    private final PartyShowcaseService partyShowcaseService;

    @GetMapping("/api/v1/parties/showcase/top3")
    public RsData<List<PartyShowcaseDto>> getTop3() {
        List<PartyShowcaseDto> top3 = partyShowcaseService.getTop3();

        return new RsData<>(
                "200-1",
                "인기 전시회 TOP3 조회 성공",
                top3
        );
    }
}
