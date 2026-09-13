package com.back.domain.party.recommendation.controller;

import com.back.domain.party.recommendation.dtos.PartyRecommendationResultDto;
import com.back.domain.party.recommendation.service.PartyRecommendationService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties/recommendations")
@RequiredArgsConstructor
public class ApiV1PartyRecommendationController {

    private final PartyRecommendationService partyRecommendationService;
    private final Rq rq;

    @GetMapping
    public RsData<PartyRecommendationResultDto> recommendations() {
        PartyRecommendationResultDto result = partyRecommendationService.getRecommendations(rq.getActorFromDb());
        return new RsData<>("200-1", "추천 파티 조회 성공", result);
    }
}
