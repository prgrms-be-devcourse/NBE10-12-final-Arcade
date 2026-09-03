package com.back.domain.goal.goal.service;

import com.back.domain.goal.goal.dtos.AssembledMemberDto;
import com.back.domain.goal.goal.dtos.GoalCreateReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailReqBody;
import com.back.domain.goal.goal.dtos.GoalUpdateReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailResponseDto;
import com.back.domain.goal.goal.dtos.GoalDto;
import com.back.domain.goal.goal.dtos.ProjectContextDto;
import com.back.domain.goal.goal.dtos.TodoContextDto;
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
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.repository.PartyPrRepository;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.repository.PersonalTodoItemRepository;
import com.back.domain.todo.todo.repository.PersonalTodoRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final PersonalTodoRepository personalTodoRepository;
    private final PersonalTodoItemRepository personalTodoItemRepository;

    @Transactional
    public GoalDto createSelfReported(Member owner, GoalCreateReqBody request) {
        GoalDetailReqBody detail = request.detail();

        Goal goal = switch (request.type()) {
            // PROJECT는 파티 확정 시 시스템이 만드는 전용 타입이라 자기신고 경로를 열지 않는다(3.6).
            // 개인 사이드 프로젝트를 남기고 싶으면 CHECKLIST를 쓴다.
            case PROJECT -> throw new ServiceException(
                    "400-4",
                    "프로젝트 성취는 파티 확정 시 자동으로 생성되므로 직접 등록할 수 없습니다."
            );
            case CONTEST -> createPersonalContest(owner, request.status(), detail);
            case CHECKLIST -> createPersonalChecklist(owner, request.status(), detail);
        };

        return new GoalDto(goalRepository.save(goal));
    }

    private PersonalContest createPersonalContest(Member owner, GoalStatus status, GoalDetailReqBody detail) {
        if (isBlank(detail.contestName())) {
            throw new ServiceException("400-4", "대회명을 입력해주세요.");
        }

        PersonalContest contest = new PersonalContest(
                owner,
                status,
                detail.contestName(),
                Boolean.TRUE.equals(detail.isTeam()),
                detail.result(),
                detail.awardDate(),
                detail.contestUrl()
        );
        contest.updateEvidenceMetadata(detail.evidenceFileName(), detail.evidenceMimeType(), detail.evidenceSize());

        return contest;
    }

    private PersonalChecklist createPersonalChecklist(Member owner, GoalStatus status, GoalDetailReqBody detail) {
        if (isBlank(detail.title())) {
            throw new ServiceException("400-4", "목표 제목을 입력해주세요.");
        }

        PersonalChecklist checklist = new PersonalChecklist(
                owner,
                status,
                detail.title(),
                detail.memo(),
                detail.targetDate()
        );
        linkTodo(checklist, owner, detail.todoId(), null);

        return checklist;
    }

    /**
     * 개인 TODO 를 성취에 연결한다. todoId 가 null 이면 연결하지 않는다(수정에서는 연결을 끊는다).
     *
     * 남의 TODO 를 연결하면 그 사람의 진행 과정이 공개되므로 소유자를 확인한다.
     * 중복 연결은 personal_todo_id UNIQUE 로도 막히지만, JOINED + IDENTITY 채번이라
     * 제약 위반이 save() 시점에 DataIntegrityViolationException 으로 터진다. 그래서 미리 거른다.
     */
    private void linkTodo(PersonalChecklist checklist, Member owner, Long todoId, Long selfGoalId) {
        if (todoId == null) {
            checklist.linkTodo(null);
            return;
        }

        PersonalTodo todo = personalTodoRepository.findById(todoId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 개인 TODO입니다."));
        todo.checkOwnedBy(owner);

        goalRepository.findChecklistByPersonalTodoId(todoId)
                .filter(linked -> !linked.getId().equals(selfGoalId))
                .ifPresent(linked -> {
                    throw new ServiceException("409-1", "이미 다른 성취에 연결된 개인 TODO입니다.");
                });

        checklist.linkTodo(todo);
    }

    /**
     * 내 성취 검색. 상태·타입·출처·연도·키워드 다섯 필터가 모두 선택이며, 넘기지 않은 조건은 쿼리에서 빠진다(기획서 9.4).
     * 본인 것만 돌려주므로 소유자 검증이 따로 필요 없다 - 조회 자체를 owner 로 건다.
     *
     * 마이페이지 연혁(components/mypage/AchievementTimeline.tsx)의 필터 네 개와 같은 축이다.
     *
     * 페이징하지 않는다 - 연혁은 이력 전체를 연도별로 묶어 보여주는 화면이라 끊어 주면 연도 그룹이 잘린다.
     */
    public List<GoalDto> getMyGoals(
            Member owner,
            GoalStatus status,
            GoalType type,
            GoalSource source,
            Integer year,
            String keyword
    ) {
        return goalRepository
                .searchMyGoals(owner, status, type, source, year, keyword)
                .stream()
                .map(GoalDto::new)
                .toList();
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
        Goal goal = findGoal(goalId);

        return new GoalDetailResponseDto(goal, buildProjectContext(goal, actor), buildTodoContext(goal));
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
        PositionType myPositionType = partyMemberRepository
                .findByPartyAndMember(party, goal.getOwner())
                .map(pm -> pm.getPosition().getType())
                .orElse(null);

        List<PartyPrDto> pullRequests = actor != null && goal.isOwnedBy(actor)
                ? partyPrRepository.findAllByPartyIdOrderByGithubUpdatedAtDesc(party.getId()).stream()
                        .map(PartyPrDto::new)
                        .toList()
                : List.of();

        return ProjectContextDto.of(party, myPositionType, partyOwner, pullRequests);
    }

    /**
     * 연결된 개인 TODO 와 그 항목들. 연결이 없으면 null 이다.
     *
     * 남의 성취에서도 채운다 - TODO 를 성취에 연결한 것 자체가 진행 과정을 공개하겠다는 뜻이다.
     * (파티 PR 목록을 본인에게만 보여주는 것과 다르다. 그쪽은 사용자가 공개를 고른 적이 없다.)
     *
     * 다만 완료한 항목만 담는다. 해낸 것은 발자취지만 아직 못 한 일은 소유자 개인의 할 일이다.
     * 그래서 진행률도 따로 내려주지 않는다 - 남은 개수를 유추할 방법이 없어야 한다.
     */
    private TodoContextDto buildTodoContext(Goal goal) {
        if (!(goal instanceof PersonalChecklist checklist)) return null;

        PersonalTodo todo = checklist.getPersonalTodo();
        if (todo == null) return null;

        return TodoContextDto.of(
                todo,
                personalTodoItemRepository.findAllByTodoAndDoneIsTrueOrderByDoneAtAscIdAsc(todo)
        );
    }

    /**
     * 성취 수정. 소유자 본인의 자기신고 성취만 고칠 수 있다(기획서 9.4).
     *
     * 파티 활동으로 자동기록된 PROJECT 성취는 크루온이 보증하는 기록이라 손댈 수 없다.
     * 사람이 고칠 수 있으면 "이 사람이 실제로 무엇을 했는지"를 보증한다는 성취의 전제가 깨진다.
     *
     * detail 을 넘겼다면 그 타입의 세부 정보를 통째로 교체한다.
     * 화면이 폼 전체를 보내오므로, 넘어오지 않은 항목은 그대로 두는 게 아니라 비운다.
     *
     * 예외
     * - 404-1 : 존재하지 않는 성취
     * - 403-1 : 남의 성취
     * - 409-1 : 자동기록(PLATFORM_VERIFIED) 성취
     * - 409-2 : 허용되지 않는 상태 전이
     * - 400-4 : 타입별 필수 필드 누락
     */
    @Transactional
    public GoalDto updateSelfReported(Member actor, long goalId, GoalUpdateReqBody request) {
        Goal goal = findGoal(goalId);

        goal.checkOwnedBy(actor);
        goal.checkModifiable();

        if (request.status() != null) {
            goal.changeStatus(request.status());
        }

        // detail 을 아예 넘기지 않으면 내용은 건드리지 않는다. 넘겼다면 그 타입의 세부 정보를 통째로 교체한다.
        if (request.detail() != null) {
            updateDetail(goal, request.detail());
        }

        // 변경분을 먼저 내보내야 @LastModifiedDate 가 찍힌다.
        // 이걸 빼면 응답의 modifyDate 가 수정 전 값(=등록 시각) 그대로 나간다.
        goalRepository.flush();

        return new GoalDto(goal);
    }

    private void updateDetail(Goal goal, GoalDetailReqBody detail) {
        // 자기신고 성취는 CONTEST/CHECKLIST 둘뿐이다. PROJECT 는 위 checkModifiable() 에서 이미 걸러졌다.
        if (goal instanceof PersonalContest contest) {
            if (isBlank(detail.contestName())) {
                throw new ServiceException("400-4", "대회명을 입력해주세요.");
            }

            contest.update(
                    detail.contestName(),
                    Boolean.TRUE.equals(detail.isTeam()),
                    detail.result(),
                    detail.awardDate(),
                    detail.contestUrl()
            );
            contest.updateEvidenceMetadata(detail.evidenceFileName(), detail.evidenceMimeType(), detail.evidenceSize());
            return;
        }

        if (goal instanceof PersonalChecklist checklist) {
            if (isBlank(detail.title())) {
                throw new ServiceException("400-4", "목표 제목을 입력해주세요.");
            }

            checklist.update(detail.title(), detail.memo(), detail.targetDate());
            linkTodo(checklist, checklist.getOwner(), detail.todoId(), checklist.getId());
            return;
        }

        throw new IllegalStateException("수정할 수 없는 성취 타입입니다: " + goal.getClass());
    }

    /**
     * 성취 삭제. 소유자 본인의 자기신고 성취만 지울 수 있다(기획서 9.4).
     *
     * 예외
     * - 404-1 : 존재하지 않는 성취
     * - 403-1 : 남의 성취
     * - 409-1 : 자동기록(PLATFORM_VERIFIED) 성취
     */
    @Transactional
    public void deleteSelfReported(Member actor, long goalId) {
        Goal goal = findGoal(goalId);

        goal.checkOwnedBy(actor);
        goal.checkDeletable();

        goalRepository.delete(goal);
    }

    private Goal findGoal(long goalId) {
        return goalRepository.findById(goalId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 성취입니다."));
    }

    /**
     * 파티 확정(모집 마감) 이벤트를 받아 참여자별 PROJECT 성취를 IN_PROGRESS로 자동 생성한다(3.6).
     * 파티 트랜잭션이 커밋된 뒤 별도 트랜잭션에서 실행되므로, 여기서 실패해도 파티 마감 자체는 되돌아가지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createProjectsForAssembledParty(
            long partyId,
            List<AssembledMemberDto> assembledMembers,
            LocalDate assembledAt
    ) {
        Map<Long, Member> membersById = memberRepository
                .findAllById(assembledMembers.stream().map(AssembledMemberDto::memberId).toList())
                .stream()
                .collect(Collectors.toMap(Member::getId, member -> member));

        // 이미 이 파티의 성취가 만들어진 사람을 한 번에 뽑아두고 메모리에서 거른다 (참여자마다 쿼리를 날리지 않기 위해)
        Set<Long> alreadyCreatedOwnerIds = Set.copyOf(
                goalRepository.findOwnerIdsBySourcePartyIdAndType(partyId, GoalType.PROJECT)
        );

        List<Project> projects = assembledMembers.stream()
                .filter(assembled -> !alreadyCreatedOwnerIds.contains(assembled.memberId()))
                .filter(assembled -> membersById.containsKey(assembled.memberId()))
                .map(assembled -> new Project(
                        membersById.get(assembled.memberId()),
                        assembled.partyAssembleToMemberId(),
                        partyId,
                        // title은 전시 게시(작업표 34번) 시점에 PARTY_SHOWCASE에서 채운다.
                        null,
                        assembled.positionType(),
                        assembledAt
                ))
                .toList();

        goalRepository.saveAll(projects);
    }

    /**
     * 파티 완료 이벤트를 받아 그 파티에서 나온 PROJECT 성취를 ACHIEVED로 전이시키고 종료일을 채운다(3.6).
     *
     * 생성 때와 같이 파티 트랜잭션이 커밋된 뒤 별도 트랜잭션에서 돈다.
     * 여기서 실패해도 파티 완료는 되돌아가지 않으므로, 다시 실행해도 안전해야 한다 -
     * 이미 ACHIEVED인 성취는 건너뛴다. (Project.complete()는 재호출 시 409-2를 던진다.)
     *
     * endDate는 이벤트가 실어온 완료 시점을 쓴다. 여기서 now()를 쓰면 재처리할 때마다 값이 달라진다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeProjectsForCompletedParty(long partyId, LocalDate completedAt) {
        goalRepository.findAllBySourcePartyIdAndType(partyId, GoalType.PROJECT).stream()
                .filter(goal -> goal.getStatus() != GoalStatus.ACHIEVED)
                .map(Project.class::cast)
                .forEach(project -> project.complete(completedAt));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
