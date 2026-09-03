package com.back.global.initData;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.goal.goal.dtos.GoalCreateReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailReqBody;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.notification.notification.entity.Notification;
import com.back.domain.notification.notification.entity.NotificationType;
import com.back.domain.notification.notification.service.NotificationService;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.service.PartyLifecycleService;
import com.back.domain.party.party.service.PartyService;
import com.back.standard.util.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class DevInitData {
    @Autowired
    @Lazy
    private DevInitData self;
    private final MemberService memberService;
    private final ContestService contestService;
    private final PartyService partyService;
    private final PartyLifecycleService partyLifecycleService;
    private final GoalService goalService;
    private final NotificationService notificationService;

    @Bean
    @Order(2)
    ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
            self.createSampleData();
            Util.cmd.runAsync(
                    "npx{{DOT_CMD}}",
                    "--yes",
                    "--package", "typescript@v5",
                    "--package", "openapi-typescript",
                    "openapi-typescript", "http://localhost:8080/v3/api-docs/apiV1",
                    "-o", "../front/src/global/backend/apiV1/schema.d.ts",
                    "--properties-required-by-default"
            );
        };
    }

    @Transactional
    public void createSampleData() {
        if (contestService.count() > 0) return;

        Member user1 = memberService.findByEmail("user1@test.com").orElseThrow();
        Member user2 = memberService.findByEmail("user2@test.com").orElseThrow();
        Member user3 = memberService.findByEmail("user3@test.com").orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        ContestResponseDto aiHackathon = contestService.write(user1, "2026 AI 서비스 해커톤", ContestFormat.HACKATHON, ContestTag.AI, today.minusDays(4), today.plusDays(18), "생성형 AI를 활용해 일상의 문제를 해결하는 48시간 팀 해커톤입니다.", "https://example.com/contests/ai-hackathon", "https://placehold.co/1200x630?text=AI+Hackathon");
        ContestResponseDto dataContest = contestService.write(user2, "공공데이터 분석 경진대회", ContestFormat.CONTEST, ContestTag.DATA, today.minusDays(10), today.plusDays(7), "공공데이터로 지역 문제를 분석하고 실행 가능한 정책 아이디어를 제안합니다.", "https://example.com/contests/public-data", "https://placehold.co/1200x630?text=Public+Data");
        ContestResponseDto fintechContest = contestService.write(user3, "핀테크 UX 챌린지", ContestFormat.CONTEST, ContestTag.FINTECH, today.plusDays(2), today.plusDays(28), "더 쉬운 금융 경험을 만드는 서비스 기획과 프로토타입을 모집합니다.", "https://example.com/contests/fintech-ux", "https://placehold.co/1200x630?text=Fintech+UX");
        contestService.write(user1, "친환경 앱 아이디어톤", ContestFormat.HACKATHON, ContestTag.ENVIRONMENT, today.minusDays(20), today.minusDays(1), "지난 친환경 아이디어톤입니다. 종료·아카이브 화면 테스트에 사용하세요.", "https://example.com/contests/green", "https://placehold.co/1200x630?text=Green+Idea");

        createInProgressParty(user1, "user1 진행 파티", "user1의 GitHub App 설치 테스트 파티", "https://github.com/example/user1-in-progress", now.plusDays(30));
        createInProgressParty(user2, "user2 진행 파티", "user2의 GitHub App 설치 테스트 파티", "https://github.com/example/user2-in-progress", now.plusDays(30));
        createInProgressParty(user3, "user3 진행 파티", "user3의 GitHub App 설치 테스트 파티", "https://github.com/example/user3-in-progress", now.plusDays(30));

        partyService.create(user1, "AI 크루", "AI 해커톤 MVP를 함께 만들 팀원을 찾습니다", "RAG 기반 학습 코치 서비스를 만들 예정입니다. 주 2회 온라인 미팅, 데모까지 함께해요.", aiHackathon.id(), null, null, TopicType.CONTEST, PartyTag.WEB, "https://github.com/example/ai-crew", 2, now.plusDays(12), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1), new PartyService.PositionCreateSpec(PositionType.FRONT, 2), new PartyService.PositionCreateSpec(PositionType.UIUX, 1)));
        partyService.create(user2, "데이터 탐험대", "공공데이터 분석 경진대회 팀원 모집", "분석 결과를 시민이 이해하기 쉬운 대시보드로 보여줄 팀입니다.", dataContest.id(), null, null, TopicType.CONTEST, PartyTag.WEB, "https://github.com/example/data-explorers", 1, now.plusDays(5), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1), new PartyService.PositionCreateSpec(PositionType.FRONT, 1), new PartyService.PositionCreateSpec(PositionType.PM, 1)));
        partyService.create(user3, "금융 UX 스프린트", "핀테크 UX 챌린지 프로토타입 팀", "Figma부터 모바일 프로토타입까지 빠르게 검증할 분을 찾습니다.", fintechContest.id(), null, null, TopicType.CONTEST, PartyTag.APP, null, 2, now.plusDays(20), List.of(new PartyService.PositionCreateSpec(PositionType.UIUX, 2), new PartyService.PositionCreateSpec(PositionType.FRONT, 1), new PartyService.PositionCreateSpec(PositionType.PM, 1)));
        partyService.create(user1, "스프링 스터디", "실전 코드리뷰 중심 Spring Boot 스터디", "매주 한 주제씩 구현하고 PR 리뷰를 진행합니다.", null, null, null, TopicType.STUDY, PartyTag.WEB, "https://github.com/example/spring-study", 1, now.plusDays(9), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3), new PartyService.PositionCreateSpec(PositionType.PM, 1)));
        partyService.create(user2, "주말 인디게임", "2주 안에 완성하는 캐주얼 게임 프로젝트", "Unity 경험이 없어도 기획과 아트, 개발을 함께 배우며 진행합니다.", null, "2026 인디게임 공모전", "https://example.com/contests/indie-game", TopicType.CONTEST, PartyTag.GAME, null, 1, now.plusDays(14), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1), new PartyService.PositionCreateSpec(PositionType.UIUX, 2), new PartyService.PositionCreateSpec(PositionType.PM, 1)));

        // GitHub App 설치·팀 공간을 확인할 수 있도록, 각 개발 계정이 파티장인 진행 중 파티를 만든다.
        // 생성 후 정식 모집 마감 흐름을 태워야 status가 IN_PROGRESS로 전환된다.

        createGoals(user1, user2, user3, today);
        createNotifications(user1, user2, user3);
    }

    private void createInProgressParty(
            Member owner,
            String partyName,
            String title,
            String githubRepoUrl,
            LocalDateTime deadline
    ) {
        PartyDto party = partyService.create(
                owner,
                partyName,
                title,
                "진행 중 파티의 GitHub App 설치와 Pull Request 동기화 흐름을 확인하기 위한 개발 데이터입니다.",
                null,
                null,
                null,
                TopicType.STUDY,
                PartyTag.WEB,
                githubRepoUrl,
                1,
                deadline,
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1))
        );
        partyLifecycleService.closeRecruiting(party.id(), owner);
    }

    private void createGoals(Member user1, Member user2, Member user3, LocalDate today) {
        goalService.createSelfReported(user1, contestGoal(
                GoalStatus.ACHIEVED, "2025 공공데이터 분석 경진대회", true,
                "우수상", today.minusMonths(3), "https://example.com/contests/public-data-2025"
        ));
        goalService.createSelfReported(user1, checklistGoal(
                GoalStatus.IN_PROGRESS, "Spring Boot 성능 최적화",
                "부하 테스트와 쿼리 튜닝을 실제 프로젝트에 적용하기", today.plusDays(30)
        ));
        goalService.createSelfReported(user2, checklistGoal(
                GoalStatus.WANT, "SQLD 자격증 취득", "주 3회 기출문제 풀이", today.plusMonths(2)
        ));
        goalService.createSelfReported(user2, contestGoal(
                GoalStatus.HOLD, "지역문제 해결 해커톤", false,
                "팀원 일정 조율 중", null, "https://example.com/contests/local-problem"
        ));
        goalService.createSelfReported(user3, checklistGoal(
                GoalStatus.ACHIEVED, "포트폴리오 리뉴얼",
                "프로젝트 3개와 디자인 시스템 정리 완료", today.minusDays(10)
        ));
    }

    private GoalCreateReqBody contestGoal(
            GoalStatus status, String contestName, boolean isTeam,
            String result, LocalDate awardDate, String contestUrl
    ) {
        return new GoalCreateReqBody(
                GoalType.CONTEST,
                status,
                new GoalDetailReqBody(
                        isTeam, result, awardDate, contestUrl,
                        null, null, null,
                        contestName, null, null, null
                )
        );
    }

    private GoalCreateReqBody checklistGoal(
            GoalStatus status, String title, String memo, LocalDate targetDate
    ) {
        return new GoalCreateReqBody(
                GoalType.CHECKLIST,
                status,
                new GoalDetailReqBody(
                        null, null, null, null,
                        null, null, null,
                        title, memo, targetDate, null
                )
        );
    }

    private void createNotifications(Member user1, Member user2, Member user3) {
        Notification readNotification = notificationService.create(user1, NotificationType.PARTY_APPLICATION_APPROVED, "AI 크루 파티 참여 신청이 승인되었습니다.");
        readNotification.read();
        notificationService.create(user1, NotificationType.PARTY_APPLICATION_APPROVED, "데이터 탐험대 파티에 새로운 지원자가 있습니다.");
        notificationService.create(user2, NotificationType.PARTY_APPLICATION_APPROVED, "스프링 스터디 파티 참여 신청이 승인되었습니다.");
        notificationService.create(user3, NotificationType.PARTY_APPLICATION_APPROVED, "금융 UX 스프린트 파티에 새로운 지원자가 있습니다.");
    }
}
