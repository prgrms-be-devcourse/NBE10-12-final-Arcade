package com.back.domain.contest.contest.controller;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.service.ContestService;
import com.back.global.rq.Rq;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contests")
@RequiredArgsConstructor
@Tag(name = "ApiV1ContestController", description = "대회 글 컨트롤러")
public class ApiV1ContestController {
    private final ContestService contestService;
    private final Rq rq;

    public List<ContestResponseDto> getAllContests() {}

}
