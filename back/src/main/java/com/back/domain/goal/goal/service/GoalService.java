package com.back.domain.goal.goal.service;

import com.back.domain.goal.goal.dtos.GoalDetailResponseDto;
import com.back.domain.goal.goal.dtos.GoalDto;
import com.back.domain.goal.goal.dtos.ProjectContextDto;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final MemberRepository memberRepository;

    // PROJECT 성취 상세는 내용이 전부 파티에 있어서, 조회 시점에 파티 쪽을 함께 읽어 조립한다.
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyPrRepository partyPrRepository;

    // 자기신고 등록 요청. 타입별로 쓰이는 필드가 달라 한 record로 받고 서비스에서 타입별 검증을 한다.
    public record SelfReportedSpec(
            GoalType type,
            GoalStatus status,
            String contestName,
            Boolean isTeam,
            String result,
            LocalDate awardDate,
            String title,
            String memo,
            LocalDate targetDate
    ) { }

    @Transactional
    public GoalDto createSelfReported(Member owner, SelfReportedSpec spec) {
        Goal goal = switch (spec.type()) {
            // PROJECT는 파티 확정 시 시스템이 만드는 전용 타입이라 자기신고 경로를 열지 않는다(3.6).
            // 개인 사이드 프로젝트를 남기고 싶으면 CHECKLIST를 쓴다.
            case PROJECT -> throw new ServiceException(
                    "400-4",
                    "프로젝트 성취는 파티 확정 시 자동으로 생성되므로 직접 등록할 수 없습니다."
            );
            case CONTEST -> createPersonalContest(owner, spec);
            case CHECKLIST -> createPersonalChecklist(owner, spec);
        };

        return new GoalDto(goalRepository.save(goal));
    }

    private PersonalContest createPersonalContest(Member owner, SelfReportedSpec spec) {
        if (isBlank(spec.contestName())) {
            throw new ServiceException("400-4", "대회명을 입력해주세요.");
        }

        return new PersonalContest(
                owner,
                spec.status(),
                spec.contestName(),
                Boolean.TRUE.equals(spec.isTeam()),
                spec.result(),
                spec.awardDate()
        );
    }

    private PersonalChecklist createPersonalChecklist(Member owner, SelfReportedSpec spec) {
        if (isBlank(spec.title())) {
            throw new ServiceException("400-4", "목표 제목을 입력해주세요.");
        }

        return new PersonalChecklist(
                owner,
                spec.status(),
                spec.title(),
                spec.memo(),
                spec.targetDate()
        );
    }

    /**
     * 내 성취 목록. 상태·타입·출처 필터는 셋 다 선택이며, 값을 넘기지 않으면 조건에서 빠진다(기획서 9.4).
     * 본인 것만 돌려주므로 소유자 검증이 따로 필요 없다 - 조회 자체를 owner 로 건다.
     */
    public Page<GoalDto> getMyGoals(
            Member owner,
            GoalStatus status,
            GoalType type,
            GoalSource source,
            Pageable pageable
    ) {
        return goalRepository
                .findAllByOwnerWithFilters(owner, status, type, source, pageable)
                .map(GoalDto::new);
    }

    /**
     * 성취 상세 조회.
     *
     * 성취는 별도의 공개 범위 설정 없이 전체 공개다(기획서 2.5, 3.7).
     * 공개 프로필에서 남의 이력을 보거나 파티장이 지원자를 심사할 때도 필요하므로 소유자로 제한하지 않는다.
     *
     * 다만 서버 인가 규칙상 아직은 로그인이 필요하다.
     * 기획서 9.4 대로 완전한 비인증 조회로 열려면 SecurityConfig 의 permitAll 에 경로를 추가해야 한다.
     *
     * 조회수(viewCount)는 여기서 올리지 않는다. 공개 노출 집계는 전시 API 가 맡는다(기획서 3.2).
     */
    public GoalDetailResponseDto getGoal(Member actor, long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 성취입니다."));

        return new GoalDetailResponseDto(goal, buildProjectContext(goal, actor));
    }

    /**
     * PROJECT 성취가 가리키는 파티 정보를 조립한다. 다른 타입이면 null 이다.
     *
     * PR 목록은 본인 성취를 볼 때만 채운다. 파티의 진행 기록은 파티원에게만 열려 있는 정보라(기획서 9.3),
     * 남의 성취를 구경하는 사람에게까지 내려주지 않는다.
     * 전시 게시가 끝난 파티는 공개해도 되지만 PARTY_SHOWCASE 가 아직 없어 판단할 수 없다.
     */
    private ProjectContextDto buildProjectContext(Goal goal, Member actor) {
        if (goal.getType() != GoalType.PROJECT || goal.getSourcePartyId() == null) return null;

        Party party = partyRepository.findById(goal.getSourcePartyId()).orElse(null);
        if (party == null) return null;

        boolean partyOwner = party.isOwnedBy(goal.getOwner());
        PositionType myPositionType = partyMemberRepository.findAllByParty(party).stream()
                .filter(pm -> pm.getMember().getId().equals(goal.getOwner().getId()))
                .map(pm -> pm.getPosition().getType())
                .findFirst()
                .orElse(null);

        List<PartyPrDto> pullRequests = actor != null && goal.isOwnedBy(actor)
                ? partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(party.getId()).stream()
                        .map(PartyPrDto::new)
                        .toList()
                : List.of();

        return ProjectContextDto.of(party, myPositionType, partyOwner, pullRequests);
    }

    /**
     * 파티 확정(모집 마감) 이벤트를 받아 참여자별 PROJECT 성취를 IN_PROGRESS로 자동 생성한다(3.6).
     * 파티 트랜잭션이 커밋된 뒤 별도 트랜잭션에서 실행되므로, 여기서 실패해도 파티 마감 자체는 되돌아가지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createProjectsForAssembledParty(long partyId, List<Long> memberIds, LocalDate assembledAt) {
        List<Member> members = memberRepository.findAllById(memberIds);

        // 이미 이 파티의 성취가 만들어진 사람을 한 번에 뽑아두고 메모리에서 거른다 (참여자마다 쿼리를 날리지 않기 위해)
        Set<Long> alreadyCreatedOwnerIds = Set.copyOf(
                goalRepository.findOwnerIdsBySourcePartyIdAndType(partyId, GoalType.PROJECT)
        );

        List<Project> projects = members.stream()
                .filter(member -> !alreadyCreatedOwnerIds.contains(member.getId()))
                .map(member -> new Project(
                        member,
                        // partyAssembleToMemberId와 positionType은 현재 이벤트에 실려오지 않아 비워 둔다.
                        // 파티 도메인 수정(docs/성취-자동생성_파티도메인_수정요청.md ②)이 반영되면 그대로 채워진다.
                        null,
                        partyId,
                        // title은 전시 게시(작업표 34번) 시점에 PARTY_SHOWCASE에서 채운다.
                        null,
                        null,
                        assembledAt
                ))
                .toList();

        goalRepository.saveAll(projects);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
