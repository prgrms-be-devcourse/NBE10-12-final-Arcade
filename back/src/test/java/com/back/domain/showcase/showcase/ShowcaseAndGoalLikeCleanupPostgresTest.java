package com.back.domain.showcase.showcase;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.goal.goal.repository.GoalRepositoryCustom.ShowcaseSort;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.interaction.like.service.LikeService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import com.back.support.PostgresTestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 전시관/좋아요 정리(자기신고 제외, 좋아요는 PARTY_SHOWCASE로만)를 실제 Postgres에서 검증한다.
// 바뀐 것: GoalRepositoryImpl.searchShowcaseGoals의 서브쿼리 IN + partyShowcase.likeCount 정렬, LikeService.likeGoal 라우팅.
@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@Transactional
class ShowcaseAndGoalLikeCleanupPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestProperties.register(registry, POSTGRES);
    }

    @Autowired
    private GoalRepository goalRepository;
    @Autowired
    private LikeService likeService;
    @Autowired
    private LikeActionRepository likeActionRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    @Test
    @DisplayName("전시관 목록: 게시된 PROJECT만 나오고 완료된 자기신고 성취는 제외된다")
    void showcaseListShowsPublishedProjectsOnly() {
        long projectId = savePublishedProject("정산 자동화 API", 9101L, 0);
        long checklistId = saveAchievedChecklist();

        Page<?> page = goalRepository.searchShowcaseGoals(null, ShowcaseSort.LATEST, PageRequest.of(0, 20));

        List<Long> ids = page.map(g -> ((com.back.domain.goal.goal.entity.Goal) g).getId()).toList();
        assertThat(ids).contains(projectId);
        assertThat(ids).doesNotContain(checklistId);
    }

    @Test
    @DisplayName("전시관 POPULAR 정렬: partyShowcase.likeCount 내림차순 (Postgres 서브쿼리 IN + 정렬)")
    void showcasePopularSortByShowcaseLikeCount() {
        long low = savePublishedProject("좋아요 적은 프로젝트", 9201L, 1);
        long high = savePublishedProject("좋아요 많은 프로젝트", 9202L, 5);

        Page<?> page = goalRepository.searchShowcaseGoals(null, ShowcaseSort.POPULAR, PageRequest.of(0, 20));

        List<Long> ids = page.map(g -> ((com.back.domain.goal.goal.entity.Goal) g).getId()).toList();
        assertThat(ids.indexOf(high)).isLessThan(ids.indexOf(low));
    }

    @Test
    @DisplayName("성취 좋아요: 게시된 PROJECT 좋아요는 PARTY_SHOWCASE 대상으로 저장된다")
    void likeGoalStoresAsPartyShowcase() {
        long goalId = savePublishedProject("정산 자동화 API", 9301L, 0);
        Member liker = memberRepository.findByEmail("user1@test.com").orElseThrow();
        long showcaseId = ((Project) goalRepository.findById(goalId).orElseThrow()).getPartyShowcase().getId();

        likeService.likeGoal(goalId, liker);

        List<LikeAction> actions = likeActionRepository.findAll();
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTargetType()).isEqualTo(TargetType.PARTY_SHOWCASE);
        assertThat(actions.get(0).getTargetId()).isEqualTo(showcaseId);
        assertThat(partyShowcaseRepository.findById(showcaseId).orElseThrow().getLikeCount()).isEqualTo(1);
    }

    private long saveAchievedChecklist() {
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();
        PersonalChecklist checklist = new PersonalChecklist(owner, GoalStatus.ACHIEVED, "자격증 취득", "메모", LocalDate.now());
        return goalRepository.save(checklist).getId();
    }

    private long savePublishedProject(String title, long partyAssembleToMemberId, int showcaseLikes) {
        Member owner = memberRepository.findByEmail("user2@test.com").orElseThrow();

        Party party = new Party(owner, "오락실 팀", "모집", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null, LocalDateTime.now().plusDays(7));
        party.addPosition(new Position(PositionType.BACK, 2));
        partyRepository.save(party);

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish(title, "설명");
        partyShowcaseRepository.save(showcase);
        for (int i = 0; i < showcaseLikes; i++) {
            partyShowcaseRepository.increaseLikeCount(showcase.getId());
        }

        Project project = new Project(owner, partyAssembleToMemberId, party.getId(), title, PositionType.BACK, LocalDate.now());
        project.complete(LocalDate.now());
        project.linkShowcase(showcase);

        return goalRepository.save(project).getId();
    }
}
