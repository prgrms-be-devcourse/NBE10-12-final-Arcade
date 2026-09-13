package com.back.domain.contest.contest.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.service.ContestService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApiV1ContestRankingController {

    private final ContestService contestService;

    @GetMapping("/api/v1/contests/top3")
    public RsData<List<ContestResponseDto>> getTop3() {
        List<ContestResponseDto> top3 = contestService.getTop3();

        return new RsData<>(
                "200-1",
                "인기 대회 TOP3 조회 성공",
                top3
        );
    }
}
