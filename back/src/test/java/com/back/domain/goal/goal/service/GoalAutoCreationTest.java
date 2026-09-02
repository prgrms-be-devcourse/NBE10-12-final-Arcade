package com.back.domain.goal.goal.service;

import com.back.domain.goal.goal.dtos.AssembledMemberDto;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 파티 확정 시 참여자별 PROJECT 성취 자동 생성 (기획서 3.6, 작업표 9번).
 *
 * 대상 메서드가 REQUIRES_NEW로 별도 트랜잭션을 열기 때문에 테스트에 @Transactional을 걸면
 * 커밋된 데이터가 롤백되지 않는다. 그래서 트랜잭션 없이 돌리고 @AfterEach에서 직접 정리한다.
 */
@ActiveProfiles("test")
@SpringBootTest
public class GoalAutoCreationTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        goalRepository.deleteAll();
    }

    /** 확정 명단 한 줄. partyAssembleToMemberId 는 PARTY_ASSEMBLE_TO_MEMBER 행의 id 다 */
    private AssembledMemberDto assembled(long memberId, long partyAssembleToMemberId, PositionType position) {
        return new AssembledMemberDto(memberId, partyAssembleToMemberId, position);
    }

    @Test
    @DisplayName("파티 확정: 참여자 수만큼 PROJECT 성취가 IN_PROGRESS로 생성된다")
    void createsOneProjectPerMember() {
        long partyId = 9001L;
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member user2 = memberRepository.findByEmail("user2@test.com").orElseThrow();
        LocalDate assembledAt = LocalDate.of(2026, 9, 1);

        goalService.createProjectsForAssembledParty(
                partyId,
                List.of(
                        assembled(user1.getId(), 5001L, PositionType.BACK),
                        assembled(user2.getId(), 5002L, PositionType.FRONT)
                ),
                assembledAt
        );

        List<Goal> goals = goalRepository.findAll();
        assertThat(goals).hasSize(2);
        assertThat(goals).allSatisfy(goal -> {
            assertThat(goal.getType()).isEqualTo(GoalType.PROJECT);
            // 아직 결과물이 없는 시작 시점이라 ACHIEVED가 아니라 IN_PROGRESS다
            assertThat(goal.getStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
            assertThat(goal.getSource()).isEqualTo(GoalSource.PLATFORM_VERIFIED);
            assertThat(goal.getSourcePartyId()).isEqualTo(partyId);

            Project project = (Project) goal;
            assertThat(project.getStartDate()).isEqualTo(assembledAt);
            assertThat(project.getEndDate()).isNull();
            // title·result는 전시 게시(작업표 34번) 시점에 PARTY_SHOWCASE에서 채워진다
            assertThat(project.getTitle()).isNull();
            assertThat(project.getResult()).isNull();
        });
        assertThat(goals).extracting(goal -> goal.getOwner().getId())
                .containsExactlyInAnyOrder(user1.getId(), user2.getId());

        // 확정 사건이 실어보낸 값이 그대로 들어간다 - 나중에 파티 구성이 바뀌어도 이 기록은 확정 당시로 남는다
        assertThat(goals).extracting(Goal::getPartyAssembleToMemberId)
                .containsExactlyInAnyOrder(5001L, 5002L);
        assertThat(goals).extracting(goal -> ((Project) goal).getPositionType())
                .containsExactlyInAnyOrder(PositionType.BACK, PositionType.FRONT);
    }

    @Test
    @DisplayName("파티 확정: 이벤트가 중복 수신돼도 같은 사람에게 성취가 두 번 생기지 않는다")
    void doesNotCreateDuplicateOnRedelivery() {
        long partyId = 9002L;
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();
        List<AssembledMemberDto> members = List.of(assembled(user1.getId(), 5003L, PositionType.BACK));

        goalService.createProjectsForAssembledParty(partyId, members, LocalDate.now());
        goalService.createProjectsForAssembledParty(partyId, members, LocalDate.now());

        assertThat(goalRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("파티 확정: 같은 사람이라도 파티가 다르면 성취가 각각 생성된다")
    void createsSeparateGoalsPerParty() {
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();
        goalService.createProjectsForAssembledParty(
                9003L, List.of(assembled(user1.getId(), 5004L, PositionType.BACK)), LocalDate.now());
        goalService.createProjectsForAssembledParty(
                9004L, List.of(assembled(user1.getId(), 5005L, PositionType.PM)), LocalDate.now());

        assertThat(goalRepository.findAll())
                .extracting(Goal::getSourcePartyId)
                .containsExactlyInAnyOrder(9003L, 9004L);
    }

    /* ---------- 파티 완료 → ACHIEVED 전이 ---------- */

    @Test
    @DisplayName("파티 완료: 그 파티의 PROJECT 성취가 ACHIEVED로 바뀌고 종료일이 채워진다")
    void completesProjectsOfCompletedParty() {
        long partyId = 9010L;
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member user2 = memberRepository.findByEmail("user2@test.com").orElseThrow();
        LocalDate completedAt = LocalDate.of(2026, 12, 24);

        goalService.createProjectsForAssembledParty(
                partyId,
                List.of(
                        assembled(user1.getId(), 5010L, PositionType.BACK),
                        assembled(user2.getId(), 5011L, PositionType.FRONT)
                ),
                LocalDate.of(2026, 9, 1)
        );

        goalService.completeProjectsForCompletedParty(partyId, completedAt);

        assertThat(goalRepository.findAll()).allSatisfy(goal -> {
            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);
            assertThat(((Project) goal).getEndDate()).isEqualTo(completedAt);
        });
    }

    /**
     * AFTER_COMMIT 리스너는 실패해도 파티 완료를 되돌리지 못하므로 재실행이 안전해야 한다.
     * Project.complete()는 이미 ACHIEVED면 409-2를 던지기 때문에, 서비스가 미리 걸러야 한다.
     */
    @Test
    @DisplayName("파티 완료: 이벤트가 중복 수신돼도 예외 없이 넘어간다")
    void completeIsIdempotent() {
        long partyId = 9011L;
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();

        goalService.createProjectsForAssembledParty(
                partyId, List.of(assembled(user1.getId(), 5012L, PositionType.BACK)), LocalDate.now());

        goalService.completeProjectsForCompletedParty(partyId, LocalDate.of(2026, 12, 24));
        // 두 번째 호출이 예외를 던지면 안 되고, 첫 번째의 종료일을 덮어써서도 안 된다
        goalService.completeProjectsForCompletedParty(partyId, LocalDate.of(2027, 1, 1));

        assertThat(goalRepository.findAll())
                .singleElement()
                .satisfies(goal -> {
                    assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);
                    assertThat(((Project) goal).getEndDate()).isEqualTo(LocalDate.of(2026, 12, 24));
                });
    }

    @Test
    @DisplayName("파티 완료: 다른 파티의 성취는 건드리지 않는다")
    void completeDoesNotTouchOtherParties() {
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();

        goalService.createProjectsForAssembledParty(
                9012L, List.of(assembled(user1.getId(), 5013L, PositionType.BACK)), LocalDate.now());
        goalService.createProjectsForAssembledParty(
                9013L, List.of(assembled(user1.getId(), 5014L, PositionType.PM)), LocalDate.now());

        goalService.completeProjectsForCompletedParty(9012L, LocalDate.of(2026, 12, 24));

        assertThat(goalRepository.findAll())
                .filteredOn(goal -> goal.getSourcePartyId() == 9013L)
                .singleElement()
                .satisfies(goal -> {
                    assertThat(goal.getStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
                    assertThat(((Project) goal).getEndDate()).isNull();
                });
    }
}
