package com.back.domain.goal.goal.service;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
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

    @Test
    @DisplayName("파티 확정: 참여자 수만큼 PROJECT 성취가 IN_PROGRESS로 생성된다")
    void createsOneProjectPerMember() {
        long partyId = 9001L;
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member user2 = memberRepository.findByEmail("user2@test.com").orElseThrow();
        LocalDate assembledAt = LocalDate.of(2026, 9, 1);

        goalService.createProjectsForAssembledParty(
                partyId,
                List.of(user1.getId(), user2.getId()),
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
            assertThat(((Project) goal).getStartDate()).isEqualTo(assembledAt);
            assertThat(((Project) goal).getEndDate()).isNull();
        });
        assertThat(goals).extracting(goal -> goal.getOwner().getId())
                .containsExactlyInAnyOrder(user1.getId(), user2.getId());
    }

    @Test
    @DisplayName("파티 확정: 이벤트가 중복 수신돼도 같은 사람에게 성취가 두 번 생기지 않는다")
    void doesNotCreateDuplicateOnRedelivery() {
        long partyId = 9002L;
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();
        List<Long> memberIds = List.of(user1.getId());

        goalService.createProjectsForAssembledParty(partyId, memberIds, LocalDate.now());
        goalService.createProjectsForAssembledParty(partyId, memberIds, LocalDate.now());

        assertThat(goalRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("파티 확정: 같은 사람이라도 파티가 다르면 성취가 각각 생성된다")
    void createsSeparateGoalsPerParty() {
        Member user1 = memberRepository.findByEmail("user1@test.com").orElseThrow();
        List<Long> memberIds = List.of(user1.getId());

        goalService.createProjectsForAssembledParty(9003L, memberIds, LocalDate.now());
        goalService.createProjectsForAssembledParty(9004L, memberIds, LocalDate.now());

        assertThat(goalRepository.findAll())
                .extracting(Goal::getSourcePartyId)
                .containsExactlyInAnyOrder(9003L, 9004L);
    }
}
