package com.back.domain.showcase.showcase.service;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.goal.goal.repository.GoalRepositoryCustom.ShowcaseSort;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.Party;
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
    public ShowcaseGoalDto toDto(Goal goal) {
        ShowcaseGoalDto.PartySummary party = null;
        // PROJECT는 Goal 자체가 아니라 sourcePartyId가 가리키는 Party에 좋아요·조회수가 집계된다
        // 같은 전시글을 참여자 수만큼 나눠 가진 Goal 각각이 서로 다른 카운트를 보여주면 안 되기 때문.
        // Goal에는 viewCount 컬럼이 없어 자기신고 성취는 셀 원천이 없고 0으로 둔다(기획서 3.2).
        int likeCount = goal.getLikeCount();
        int viewCount = 0;
        PositionType positionType = null;

        // PROJECT만 파티 요약을 같이 채운다 - sourcePartyId/partyShowcase가 있는 유일한 타입.
        if (goal instanceof Project projectGoal) {
            Party sourceParty = projectGoal.getPartyShowcase().getParty();

            party = new ShowcaseGoalDto.PartySummary(sourceParty.getId(), sourceParty.getPartyName());
            likeCount = projectGoal.getPartyShowcase().getLikeCount();
            viewCount = projectGoal.getPartyShowcase().getViewCount();
            positionType = projectGoal.getPositionType();
        }

        return new ShowcaseGoalDto(
                goal.getId(),
                party,
                goal.getType(),
                goal.getStatus(),
                goal.getSource(),
                new ShowcaseGoalDto.Detail(goal.getTitle()),
                positionType,
                likeCount,
                viewCount,
                goal.getCreateDate()
        );
    }
}
