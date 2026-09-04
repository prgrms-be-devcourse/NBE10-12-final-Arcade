package com.back.domain.showcase.showcase.service;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.goal.goal.repository.GoalRepositoryCustom.ShowcaseSort;
import com.back.domain.showcase.showcase.dtos.ShowcaseGoalDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowcaseService {

    private final GoalRepository goalRepository;

    public Page<ShowcaseGoalDto> getShowcaseGoals(GoalType type, ShowcaseSort sort, Pageable pageable) {
        ShowcaseSort resolvedSort = sort != null ? sort : ShowcaseSort.LATEST;

        return goalRepository.searchShowcaseGoals(type, resolvedSort, pageable)
                .map(this::toDto);
    }

    private ShowcaseGoalDto toDto(Goal goal) {
        ShowcaseGoalDto.PartySummary party = null;
        String title;

        if (goal instanceof Project projectGoal) {
            title = projectGoal.getTitle();
            party = new ShowcaseGoalDto.PartySummary(
                    projectGoal.getPartyShowcase().getParty().getId(),
                    projectGoal.getPartyShowcase().getParty().getPartyName()
            );
        } else if (goal instanceof PersonalContest contestGoal) {
            title = contestGoal.getTitle();
        } else if (goal instanceof PersonalChecklist checklistGoal) {
            title = checklistGoal.getTitle();
        } else {
            title = null;
        }

        return new ShowcaseGoalDto(
                goal.getId(),
                party,
                goal.getType(),
                goal.getStatus(),
                goal.getSource(),
                new ShowcaseGoalDto.Detail(title),
                goal.getLikeCount(),
                goal.getCreateDate()
        );
    }
}
