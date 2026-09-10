package com.back.domain.showcase.showcase.service;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.goal.goal.repository.GoalRepositoryCustom.ShowcaseSort;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.showcase.entity.PartyShowcase;
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

    // 전시관 목록과 북마크함이 같은 카드를 그리므로 조립을 공유한다.
    // 좋아요·조회수는 Goal이 아니라 그 파티가 공유하는 PARTY_SHOWCASE에 집계된다(기획서 3.2) -
    // 같은 전시글을 참여자 수만큼 나눠 가진 Goal 각각이 다른 카운트를 보여주면 안 되기 때문.
    public ShowcaseGoalDto toDto(Goal goal) {
        if (!(goal instanceof Project projectGoal)) {
            throw new IllegalStateException("전시 카드 대상은 게시된 PROJECT 성취뿐입니다: goalId=" + goal.getId());
        }

        PartyShowcase showcase = projectGoal.getPartyShowcase();
        Party sourceParty = showcase.getParty();

        return new ShowcaseGoalDto(
                goal.getId(),
                new ShowcaseGoalDto.PartySummary(sourceParty.getId(), sourceParty.getPartyName()),
                goal.getType(),
                goal.getStatus(),
                goal.getSource(),
                new ShowcaseGoalDto.Detail(goal.getTitle()),
                projectGoal.getPositionType(),
                showcase.getLikeCount(),
                showcase.getViewCount(),
                goal.getCreateDate()
        );
    }
}
