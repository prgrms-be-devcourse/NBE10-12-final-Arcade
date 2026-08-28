package com.back.domain.contest.contest.controller;

import com.back.domain.contest.contest.service.ContestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contests")
@RequiredArgsConstructor
public class ApiV1ContestController {

    private final ContestService contestService;

}
