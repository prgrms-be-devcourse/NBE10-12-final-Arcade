package com.back.domain.party.recommendation.controller;

import com.back.domain.party.recommendation.service.PartyRecommendationBatchService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//로컬/dev에서 배치를 10시 5분까지 안 기다리고 즉시 확인하기 위한 임시 트리거.
//custom.dev-tools.enabled=true 인 환경에서만 뜬다 (기본 false, 운영 도메인에는 설정 안 함).
@ConditionalOnProperty(name = "custom.dev-tools.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/v1/dev/recommendations")
@RequiredArgsConstructor
public class DevPartyRecommendationController {

    private final PartyRecommendationBatchService batchService;
    private final Rq rq;

    @PostMapping("/compute-me")
    public RsData<Void> computeForMe() {
        batchService.computeForMember(rq.getActorFromDb());
        return new RsData<>("200-1", "추천 재계산 완료", null);
    }
}
