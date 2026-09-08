package com.back.domain.member.profile.service;

import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.dtos.MemberSummaryDto;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSummaryService {

    private final PartyMemberRepository partyMemberRepository;
    private final PartyShowcaseRepository partyShowcaseRepository;
    private final GoalRepository goalRepository;
    private final ActivityLogService activityLogService;

    public MemberSummaryDto summary(Member actor) {
        ActivityLogService.Summary activity = activityLogService.summary(actor);

        return new MemberSummaryDto(
                partyMemberRepository.countByMemberAndStatusAndPartyStatus(
                        actor, PartyMemberStatus.APPROVED, PartyStatus.COMPLETED),
                goalRepository.countByOwnerAndTypeAndStatus(
                        actor, GoalType.CONTEST, GoalStatus.ACHIEVED),
                partyShowcaseRepository.countPublishedByMemberAndStatus(
                        actor, PartyMemberStatus.APPROVED),
                activity.streakDays(),
                activity.heatmap(),
                List.of()
        );
    }
}
