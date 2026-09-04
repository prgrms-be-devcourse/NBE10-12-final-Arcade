package com.back.domain.showcase.showcase.controller;

import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.repository.GoalRepositoryCustom.ShowcaseSort;
import com.back.domain.showcase.showcase.dtos.ShowcaseGoalDto;
import com.back.domain.showcase.showcase.service.ShowcaseService;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApiV1ShowcaseController {

    private final ShowcaseService showcaseService;

    @GetMapping("/api/v1/showcase/goals")
    public RsData<Page<ShowcaseGoalDto>> getShowcaseGoals(
            @RequestParam(required = false) GoalType type,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ShowcaseSort parsedSort = parseSort(sort);

        Page<ShowcaseGoalDto> goals = showcaseService.getShowcaseGoals(
                type, parsedSort, PageRequest.of(page, size)
        );

        return new RsData<>(
                "200-1",
                "전시 성취 목록 조회 성공",
                goals
        );
    }

    private ShowcaseSort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        try {
            return ShowcaseSort.valueOf(sort);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("400-1", "sort 값은 POPULAR 또는 LATEST여야 합니다.");
        }
    }
}
