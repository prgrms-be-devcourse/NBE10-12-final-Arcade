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
        // PROJECT는 Goal 자체가 아니라 sourcePartyId가 가리키는 Party에 좋아요가 집계된다
        // 같은 전시글을 참여자 수만큼 나눠 가진 Goal 각각이 서로 다른 카운트를 보여주면 안 되기 때문
        int likeCount = goal.getLikeCount();

        // 세 하위 타입 다 title을 갖고 있지만 공통 상위 타입엔 없어서 분기해서 꺼낸다.
        // PROJECT만 파티 요약을 같이 채운다 - sourcePartyId/partyShowcase가 있는 유일한 타입.
        if (goal instanceof Project projectGoal) {
            title = projectGoal.getTitle();
            party = new ShowcaseGoalDto.PartySummary(
                    projectGoal.getPartyShowcase().getParty().getId(),
                    projectGoal.getPartyShowcase().getParty().getPartyName()
            );
            likeCount = projectGoal.getPartyShowcase().getParty().getLikeCount();
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
                likeCount,
                goal.getCreateDate()
        );
    }
}
